package abms.smartMeeting;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RoomAgent extends BaseAgent implements SteppableEntity, ShardContainer {
    private static final long serialVersionUID = 1L;
    private static final AgentShardDesignation ENVIRONMENT =
            AgentShardDesignation.customShard("Environment");

    private EnvironmentLinkShard e = new EnvironmentLinkShard();
    private String roomId;
    private int capacity = 6;
    private Set<EquipmentType> equipment = new LinkedHashSet<>();
    private List<Reservation> reservations = new ArrayList<>();
    private boolean occupied = false;
    private Queue<AgentWave> incomingWaves = new ConcurrentLinkedQueue<>();
    private String nodeId = "unknown";
    private boolean waitingForAuctionMessage;

    public RoomAgent() {
        e.addGeneralContext(this);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.containsKey("roomId"))
            roomId = configuration.getAValue("roomId");
        capacity = readInt(configuration, "capacity", capacity);
        if (configuration.containsKey("equipment"))
            equipment = EquipmentType.parseSet(configuration.getAValue("equipment"));
        if (configuration.containsKey("nodeId"))
            nodeId = configuration.getAValue("nodeId");
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        e.addGeneralContext(context);
        return super.addGeneralContext(context);
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        if (event.getType() == AgentEvent.AgentEventType.AGENT_WAVE && event instanceof AgentWave) {
            incomingWaves.add((AgentWave) event);
            return true;
        }
        return false;
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        if (ENVIRONMENT.equals(designation))
            return e;
        return AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)
                .equals(designation) ? e.getMessagingShard() : null;
    }

    @Override
    public boolean start() {
        if (!super.start())
            return false;
        if (e.getMessagingShard() != null)
            e.getMessagingShard().signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public void step() {
        if (waitingForAuctionMessage && incomingWaves.isEmpty())
            return;
        occupied = reservations.stream().anyMatch(r -> r.getStatus() == ReservationStatus.CONFIRMED);
        AgentWave wave;
        while ((wave = incomingWaves.poll()) != null) {
            try {
                SmartMeetingMessageType type = SmartMeetingMessageCodec.decodeType(wave);
                switch (type) {
                    case REQUEST_FOR_PROPOSALS:
                        handleRequestForProposals(wave);
                        break;
                    case ACCEPT_BID:
                        handleAcceptBid(wave);
                        break;
                    case REJECT_BID:
                        handleRejectBid(wave);
                        break;
                    case RELEASE_ROOM:
                        handleReleaseRoom(wave);
                        break;
                    default:
                        break;
                }
            } catch (IllegalArgumentException ignored) {
                // Message belongs to another scenario.
            }
        }
    }

    private void handleRequestForProposals(AgentWave wave) {
        MeetingRequest request = SmartMeetingMessageCodec.decodeMeetingRequest(wave);
        RoomBid bid = evaluateRequest(request);
        boolean sent = e.sendTo(request.getRequesterName(), SmartMeetingMessageCodec.encodeBid(bid));
        if (!sent)
            ScenarioTrace.record(getEntityName(), "RoomAgent", "message-send-failed",
                    request.getRequestId(), request.getRequesterName(), Boolean.FALSE, "delivery-failed", nodeId);
        li("room [] bid for [] feasible: [] score: []", getRoomId(), request.getRequestId(),
                Boolean.valueOf(bid.isFeasible()), Integer.valueOf(bid.getScore()));
        ScenarioTrace.record(getEntityName(), "RoomAgent", "bid-created",
                request.getRequestId(), getRoomId(), Boolean.valueOf(bid.isFeasible()), bid.getReason(), nodeId);
        waitingForAuctionMessage = true;
    }

    private void handleAcceptBid(AgentWave wave) {
        RoomBid bid = SmartMeetingMessageCodec.decodeRoomBid(wave);
        if (!getEntityName().equals(bid.getRoomAgentName()))
            return;
        Reservation reservation = lockReservation(bid);
        reservation.confirm();
        waitingForAuctionMessage = false;
        occupied = true;
        li("room [] confirmed reservation [] for request []", getRoomId(), reservation.getReservationId(),
                reservation.getRequestId());
        ScenarioTrace.record(getEntityName(), "RoomAgent", "reservation-confirmed",
                reservation.getRequestId(), getRoomId(), Boolean.TRUE, null, nodeId);
        // The run may end at any time relative to other nodes, so persist the trace now.
        ScenarioTrace.exportRun();
    }

    private void handleRejectBid(AgentWave wave) {
        waitingForAuctionMessage = false;
        String requestId = wave.get("requestId");
        for (Reservation reservation : reservations)
            if (reservation.getRequestId().equals(requestId) && reservation.getStatus() == ReservationStatus.LOCKED)
                reservation.release();
    }

    private void handleReleaseRoom(AgentWave wave) {
        waitingForAuctionMessage = false;
        releaseReservation(SmartMeetingMessageCodec.decodeReservationId(wave));
    }

    private RoomBid evaluateRequest(MeetingRequest request) {
        if (!canHost(request))
            return new RoomBid(request.getRequestId(), getEntityName(), getRoomId(), false, 0,
                    request.getPreferredSlot(), explainRejection(request));
        return new RoomBid(request.getRequestId(), getEntityName(), getRoomId(), true,
                computeSuitabilityScore(request), request.getPreferredSlot(), "available");
    }

    private boolean canHost(MeetingRequest request) {
        return request.getAttendees() <= capacity
                && equipment.containsAll(request.getRequiredEquipment())
                && isAvailable(request.getPreferredSlot());
    }

    private boolean isAvailable(TimeSlot slot) {
        for (Reservation reservation : reservations)
            if (reservation.conflictsWith(slot))
                return false;
        return true;
    }

    private int computeSuitabilityScore(MeetingRequest request) {
        int score = request.getPriority() * 100;
        score += Math.max(0, capacity - request.getAttendees()) <= 2 ? 25 : 10;
        score += request.getRequiredEquipment().size() * 15;
        score -= Math.max(0, capacity - request.getAttendees());
        return score;
    }

    private Reservation lockReservation(RoomBid bid) {
        Reservation existing = findReservationByRequest(bid.getRequestId());
        if (existing != null)
            return existing;
        Reservation reservation = new Reservation("RES-" + getRoomId() + "-" + bid.getRequestId(),
                bid.getRequestId(), getRoomId(), bid.getProposedSlot(), equipment);
        reservation.lock();
        reservations.add(reservation);
        return reservation;
    }

    private void releaseReservation(String reservationId) {
        if (reservationId == null)
            return;
        for (Reservation reservation : reservations)
            if (reservationId.equals(reservation.getReservationId())) {
                reservation.release();
                li("room [] released reservation []", getRoomId(), reservationId);
            }
        occupied = reservations.stream().anyMatch(r -> r.getStatus() == ReservationStatus.CONFIRMED);
    }

    private Reservation findReservationByRequest(String requestId) {
        for (Reservation reservation : reservations)
            if (reservation.getRequestId().equals(requestId)
                    && reservation.getStatus() != ReservationStatus.RELEASED)
                return reservation;
        return null;
    }

    private String explainRejection(MeetingRequest request) {
        if (request.getAttendees() > capacity)
            return "capacity";
        if (!equipment.containsAll(request.getRequiredEquipment()))
            return "equipment";
        if (!isAvailable(request.getPreferredSlot()))
            return "slot";
        return "unavailable";
    }

    public String getRoomId() {
        return roomId != null ? roomId : getName();
    }

    public int getCapacity() {
        return capacity;
    }

    public Set<EquipmentType> getEquipment() {
        return equipment;
    }

    public boolean isOccupied() {
        return occupied;
    }

    private static int readInt(MultiTreeMap configuration, String key, int fallback) {
        if (configuration == null || !configuration.containsKey(key))
            return fallback;
        try {
            return Integer.parseInt(configuration.getAValue(key));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public String getEntityName() {
        return getRoomId() != null ? getRoomId() : "Room";
    }
}
