package abms.smartMeeting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.graph.GraphTopology;
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
    private Queue<AgentWave> incomingWaves = new LinkedList<>();

    // Auction state
    private enum AuctionState {IDLE, COLLECTING_BIDS}

    private AuctionState auctionState = AuctionState.IDLE;
    private MeetingRequest currentRequest;
    private String currentRequesterName;
    private Map<String, RoomBid> receivedBids = new LinkedHashMap<>();
    private int bidWaitSteps = 7;
    private int currentWaitStep = 0;

    // Active reservations for release tracking
    private int releaseAfterSteps = 15;
    private List<ActiveReservation> activeReservations = new ArrayList<>();

    // Queue of pending booking requests from PersonAgents
    private Queue<AgentWave> pendingBookingRequests = new LinkedList<>();

    // Simulation reference for finding PersonAgents
    private Simulation simulation;

    // Per-run stats
    private int currentStep = 0;
    private int auctionStartedStep = -1;
    private final List<AuctionOutcome> outcomes = new ArrayList<>();
    private final Map<String, Integer> winsPerRoom = new LinkedHashMap<>();

    public AuctionAgent() {
        e.addGeneralContext(this);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        releaseAfterSteps = readInt(configuration, "releaseAfterSteps", releaseAfterSteps);
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof Simulation)
            simulation = (Simulation) context;
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
        currentStep++;
        computeBidWaitIfNeeded();
        releaseExpiredReservations();
        processIncomingWaves();

        switch (auctionState) {
            case IDLE:
                AgentWave bookingWave = pendingBookingRequests.poll();
                if (bookingWave == null)
                    return;
                MeetingRequest originalRequest = SmartMeetingMessageCodec.decodeMeetingRequest(bookingWave);
                currentRequesterName = originalRequest.getRequesterName();
                // Replace requester with auction agent's name so rooms send bids back here
                currentRequest = new MeetingRequest(originalRequest.getRequestId(), getEntityName(),
                        originalRequest.getAttendees(), originalRequest.getDurationMinutes(),
                        originalRequest.getPreferredSlot(), originalRequest.getRequiredEquipment(),
                        originalRequest.getPriority());
                receivedBids.clear();
                currentWaitStep = 0;
                auctionStartedStep = currentStep;
                auctionState = AuctionState.COLLECTING_BIDS;
                broadcastRFP(currentRequest);
                li("started auction for [] from person [] (waiting [] steps for bids)",
                        currentRequest.getRequestId(), currentRequesterName,
                        Integer.valueOf(bidWaitSteps));
                break;

            case COLLECTING_BIDS:
                currentWaitStep++;
                if (currentWaitStep < bidWaitSteps)
                    return;
                resolveAuction();
                auctionState = AuctionState.IDLE;
                break;
        }
    }

    private void processIncomingWaves() {
        while (!incomingWaves.isEmpty()) {
            AgentWave wave = incomingWaves.poll();
            try {
                SmartMeetingMessageType type = SmartMeetingMessageCodec.decodeType(wave);
                switch (type) {
                    case BOOKING_REQUEST:
                        pendingBookingRequests.add(wave);
                        break;
                    case BID:
                        handleBid(wave);
                        break;
                    default:
                        break;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void handleBid(AgentWave wave) {
        RoomBid bid = SmartMeetingMessageCodec.decodeRoomBid(wave);
        if (currentRequest == null || !currentRequest.getRequestId().equals(bid.getRequestId()))
            return;
        receivedBids.put(bid.getRoomAgentName(), bid);
    }

    private void broadcastRFP(MeetingRequest request) {
        // Send RFP to each room individually via graph-routed hop-by-hop delivery
        if (simulation == null)
            return;
        for (Entity<?> entity : simulation.getSimulationObjects()) {
            if (entity instanceof RoomAgent)
                e.sendWaveTo(entity.asContext(),
                        SmartMeetingMessageCodec.encodeRequestForProposals(request));
        }
    }

    private void resolveAuction() {
        List<RoomBid> bids = new ArrayList<>(receivedBids.values());
        RoomBid winner = selectBestBid(bids);

        EntityProxy<?> personRef = findPersonAgent(currentRequesterName);
        int feasibleCount = 0;
        for (RoomBid bid : bids)
            if (bid.isFeasible()) feasibleCount++;

        if (winner != null) {
            // Send accept to winning room (hop-by-hop through graph)
            EntityProxy<?> roomRef = findGraphEntity(winner.getRoomAgentName());
            if (roomRef != null)
                e.sendWaveTo(roomRef, SmartMeetingMessageCodec.encodeAcceptBid(winner));
            activeReservations.add(new ActiveReservation(winner.getRoomAgentName(),
                    "RES-" + winner.getRoomId() + "-" + winner.getRequestId(), releaseAfterSteps));

            // Reject losing bids
            for (RoomBid bid : bids) {
                if (bid.getRoomAgentName().equals(winner.getRoomAgentName()))
                    continue;
                EntityProxy<?> loserRef = findGraphEntity(bid.getRoomAgentName());
                if (loserRef != null)
                    e.sendWaveTo(loserRef, SmartMeetingMessageCodec.encodeRejectBid(bid.getRequestId()));
            }

            // Respond to person (direct, 1 step)
            if (personRef != null)
                e.sendDirect(personRef, SmartMeetingMessageCodec.encodeBookingResponse(
                        currentRequest.getRequestId(), true, winner.getRoomId(), null));

            li("auction [] WON by room [] (score []) from [] bids",
                    currentRequest.getRequestId(), winner.getRoomId(),
                    Integer.valueOf(winner.getScore()), Integer.valueOf(bids.size()));
            winsPerRoom.merge(winner.getRoomId(), 1, Integer::sum);
            outcomes.add(new AuctionOutcome(currentRequest.getRequestId(), currentRequesterName,
                    auctionStartedStep, currentStep, true, winner.getRoomId(), winner.getScore(),
                    bids.size(), feasibleCount, null));
        } else {
            if (personRef != null)
                e.sendDirect(personRef, SmartMeetingMessageCodec.encodeBookingResponse(
                        currentRequest.getRequestId(), false, null, "no feasible room"));
            li("auction [] FAILED — no feasible bid from [] responses",
                    currentRequest.getRequestId(), Integer.valueOf(bids.size()));
            outcomes.add(new AuctionOutcome(currentRequest.getRequestId(), currentRequesterName,
                    auctionStartedStep, currentStep, false, null, 0, bids.size(), feasibleCount,
                    "no feasible room"));
        }

        currentRequest = null;
        currentRequesterName = null;
        auctionStartedStep = -1;
    }

    /**
     * Selects the highest-scoring feasible bid. When two or more bids tie on score the
     * winner is drawn uniformly at random from the tied set, so that across many runs the
     * winner distribution is not anchored to entity-set iteration order.
     */
    private RoomBid selectBestBid(List<RoomBid> bids) {
        int bestScore = Integer.MIN_VALUE;
        List<RoomBid> bestTier = new ArrayList<>();
        for (RoomBid bid : bids) {
            if (!bid.isFeasible()) continue;
            if (bid.getScore() > bestScore) {
                bestScore = bid.getScore();
                bestTier.clear();
                bestTier.add(bid);
            } else if (bid.getScore() == bestScore) {
                bestTier.add(bid);
            }
        }
        if (bestTier.isEmpty()) return null;
        if (bestTier.size() == 1) return bestTier.get(0);
        return bestTier.get(e.nextInt(bestTier.size()));
    }

    public List<AuctionOutcome> getOutcomes() {
        return Collections.unmodifiableList(outcomes);
    }

    public Map<String, Integer> getWinsPerRoom() {
        return Collections.unmodifiableMap(winsPerRoom);
    }

    private void releaseExpiredReservations() {
        List<ActiveReservation> released = new ArrayList<>();
        for (ActiveReservation reservation : activeReservations) {
            reservation.remainingSteps--;
            if (reservation.remainingSteps > 0)
                continue;
            EntityProxy<?> target = findGraphEntity(reservation.roomAgentName);
            if (target != null) {
                e.sendWaveTo(target, SmartMeetingMessageCodec.encodeReleaseRoom(reservation.reservationId));
                li("released reservation [] for room []", reservation.reservationId, reservation.roomAgentName);
            }
            released.add(reservation);
        }
        activeReservations.removeAll(released);
    }

    private void computeBidWaitIfNeeded() {
        if (bidWaitSteps > 0 && bidWaitSteps != 7)
            return; // already computed
        if (e.getTopology() instanceof GraphTopology) {
            int diameter = ((GraphTopology) e.getTopology()).getDiameter();
            bidWaitSteps = 2 * diameter + 1;
            li("computed bidWaitSteps = [] (graph diameter = [])",
                    Integer.valueOf(bidWaitSteps), Integer.valueOf(diameter));
        }
    }

    private EntityProxy<?> findGraphEntity(String entityName) {
        if (entityName == null || simulation == null)
            return null;
        for (Entity<?> entity : simulation.getSimulationObjects())
            if (entityName.equals(entity.asContext().getEntityName()))
                return entity.asContext();
        return null;
    }

    private EntityProxy<?> findPersonAgent(String personName) {
        return findGraphEntity(personName);
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

    public static class AuctionOutcome {
        public final String requestId;
        public final String requesterName;
        public final int startStep;
        public final int resolutionStep;
        public final boolean won;
        public final String winnerRoomId;
        public final int winnerScore;
        public final int bidsReceived;
        public final int feasibleBids;
        public final String failureReason;

        public AuctionOutcome(String requestId, String requesterName, int startStep, int resolutionStep,
                boolean won, String winnerRoomId, int winnerScore, int bidsReceived, int feasibleBids,
                String failureReason) {
            this.requestId = requestId;
            this.requesterName = requesterName;
            this.startStep = startStep;
            this.resolutionStep = resolutionStep;
            this.won = won;
            this.winnerRoomId = winnerRoomId;
            this.winnerScore = winnerScore;
            this.bidsReceived = bidsReceived;
            this.feasibleBids = feasibleBids;
            this.failureReason = failureReason;
        }

        public int latencySteps() {
            return resolutionStep - startStep;
        }
    }
}
