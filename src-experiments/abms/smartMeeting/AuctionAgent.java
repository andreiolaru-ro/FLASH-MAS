package abms.smartMeeting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class AuctionAgent extends BaseAgent implements SteppableEntity, ShardContainer {
    private static final long serialVersionUID = 1L;
    private static final AgentShardDesignation ENVIRONMENT =
            AgentShardDesignation.customShard("Environment");

    private EnvironmentLinkShard e = new EnvironmentLinkShard();
    private int visionRange = 4;
    private Queue<AgentWave> incomingWaves = new LinkedList<>();
    private MeetingRequest currentRequest;
    private Map<String, RoomBid> receivedBids = new LinkedHashMap<>();
    private int bidWaitSteps = 2;
    private int currentWaitStep = 0;
    private int requestCounter = 0;
    private int cooldownSteps = 4;
    private int currentCooldown = 0;
    private int releaseAfterSteps = 8;
    private List<ActiveReservation> activeReservations = new ArrayList<>();

    public AuctionAgent() {
        e.addGeneralContext(this);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        visionRange = readInt(configuration, "visionRange", visionRange);
        bidWaitSteps = readInt(configuration, "bidWaitSteps", bidWaitSteps);
        cooldownSteps = readInt(configuration, "cooldownSteps", cooldownSteps);
        releaseAfterSteps = readInt(configuration, "releaseAfterSteps", releaseAfterSteps);
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
        return ENVIRONMENT.equals(designation) ? e : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public void step() {
        releaseExpiredReservations();
        processIncomingWaves();
        if (currentRequest == null) {
            if (currentCooldown > 0) {
                currentCooldown--;
                return;
            }
            currentRequest = createUserRequest();
            receivedBids.clear();
            currentWaitStep = 0;
            broadcastRequestForProposals(currentRequest);
            li("created meeting request [] for [] attendees", currentRequest.getRequestId(),
                    Integer.valueOf(currentRequest.getAttendees()));
            return;
        }

        currentWaitStep++;
        if (currentWaitStep < bidWaitSteps)
            return;

        List<RoomBid> bids = collectBids();
        RoomBid winner = selectBestBid(bids);
        if (winner != null) {
            acceptBid(winner);
            rememberActiveReservation(winner);
            rejectLosingBids(winner);
            li("accepted room [] from [] with score []", winner.getRoomId(), winner.getRoomAgentName(),
                    Integer.valueOf(winner.getScore()));
        } else {
            li("no feasible bid for request []", currentRequest.getRequestId());
        }
        currentRequest = null;
        currentCooldown = cooldownSteps;
    }

    private void processIncomingWaves() {
        while (!incomingWaves.isEmpty()) {
            AgentWave wave = incomingWaves.poll();
            try {
                if (SmartMeetingMessageCodec.decodeType(wave) == SmartMeetingMessageType.BID)
                    handleBid(wave);
            } catch (IllegalArgumentException ignored) {
                // Message belongs to another scenario.
            }
        }
    }

    private MeetingRequest createUserRequest() {
        int attendees = 2 + e.nextInt(9);
        int duration = e.nextBoolean() ? 30 : 60;
        int start = (9 * 60) + (e.nextInt(8) * 30);
        Set<EquipmentType> equipment = new LinkedHashSet<>();
        if (e.nextBoolean())
            equipment.add(EquipmentType.PROJECTOR);
        if (e.nextInt(4) == 0)
            equipment.add(EquipmentType.VIDEO_CONFERENCE);
        if (e.nextInt(5) == 0)
            equipment.add(EquipmentType.WHITEBOARD);
        return new MeetingRequest("REQ-" + (++requestCounter), getEntityName(), attendees, duration,
                new TimeSlot(start, start + duration), equipment, 1 + e.nextInt(3));
    }

    private void broadcastRequestForProposals(MeetingRequest request) {
        for (EntityProxy<?> entity : getVisibleEntities()) {
            if (entity instanceof RoomAgent)
                e.sendWaveTo(entity, SmartMeetingMessageCodec.encodeRequestForProposals(request));
        }
    }

    private void handleBid(AgentWave wave) {
        RoomBid bid = SmartMeetingMessageCodec.decodeRoomBid(wave);
        if (currentRequest == null || !currentRequest.getRequestId().equals(bid.getRequestId()))
            return;
        receivedBids.put(bid.getRoomAgentName(), bid);
    }

    private List<RoomBid> collectBids() {
        return new ArrayList<>(receivedBids.values());
    }

    private static RoomBid selectBestBid(List<RoomBid> bids) {
        RoomBid best = null;
        for (RoomBid bid : bids) {
            if (!bid.isFeasible())
                continue;
            if (best == null || bid.getScore() > best.getScore())
                best = bid;
        }
        return best;
    }

    private void acceptBid(RoomBid bid) {
        EntityProxy<?> target = findVisibleEntity(bid.getRoomAgentName());
        if (target != null)
            e.sendWaveTo(target, SmartMeetingMessageCodec.encodeAcceptBid(bid));
    }

    private void rememberActiveReservation(RoomBid bid) {
        activeReservations.add(new ActiveReservation(bid.getRoomAgentName(),
                buildReservationId(bid), releaseAfterSteps));
    }

    private void releaseExpiredReservations() {
        List<ActiveReservation> released = new ArrayList<>();
        for (ActiveReservation reservation : activeReservations) {
            reservation.remainingSteps--;
            if (reservation.remainingSteps > 0)
                continue;

            EntityProxy<?> target = findVisibleEntity(reservation.roomAgentName);
            if (target != null) {
                e.sendWaveTo(target, SmartMeetingMessageCodec.encodeReleaseRoom(reservation.reservationId));
                li("released reservation [] for room agent []", reservation.reservationId,
                        reservation.roomAgentName);
            }
            released.add(reservation);
        }
        activeReservations.removeAll(released);
    }

    private void rejectLosingBids(RoomBid winner) {
        for (RoomBid bid : receivedBids.values()) {
            if (winner.getRoomAgentName().equals(bid.getRoomAgentName()))
                continue;
            EntityProxy<?> target = findVisibleEntity(bid.getRoomAgentName());
            if (target != null)
                e.sendWaveTo(target, SmartMeetingMessageCodec.encodeRejectBid(bid.getRequestId()));
        }
    }

    private EntityProxy<?> findVisibleEntity(String entityName) {
        for (EntityProxy<?> entity : getVisibleEntities())
            if (entityName != null && entityName.equals(entity.getEntityName()))
                return entity;
        return null;
    }

    private static String buildReservationId(RoomBid bid) {
        return "RES-" + bid.getRoomId() + "-" + bid.getRequestId();
    }

    private Set<EntityProxy<?>> getVisibleEntities() {
        Set<EntityProxy<?>> result = new LinkedHashSet<>();
        Position current = e.getCurrentPosition();
        if (current != null)
            result.addAll(e.getEntitiesAt(current));
        result.addAll(e.getEntitiesInVicinity());
        for (Set<EntityProxy<?>> entities : e.observe(visionRange).values())
            result.addAll(entities);
        return result;
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
        return getName() != null ? getName() : "Auction";
    }

    private static class ActiveReservation {
        final String roomAgentName;
        final String reservationId;
        int remainingSteps;

        ActiveReservation(String roomAgentName, String reservationId, int remainingSteps) {
            this.roomAgentName = roomAgentName;
            this.reservationId = reservationId;
            this.remainingSteps = remainingSteps;
        }
    }
}
