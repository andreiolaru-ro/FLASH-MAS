package abms.smartMeeting;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.Simulation;
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

public class AuctionAgent extends BaseAgent implements SteppableEntity, ShardContainer {
    private static final long serialVersionUID = 1L;
    private static final AgentShardDesignation ENVIRONMENT =
            AgentShardDesignation.customShard("Environment");
    private static final long DEFAULT_STEP_PERIOD_MS = 100;
    /** Steps to collect bids before resolving with what arrived. */
    private static final long DEFAULT_BID_TIMEOUT = 300;
    /** Steps between resending the RFP to rooms that have not answered yet. */
    private static final long DEFAULT_RFP_RETRY_INTERVAL = 20;

    private EnvironmentLinkShard e = new EnvironmentLinkShard();
    /** Time between own steps (ms), when the agent is started outside a simulation executor. */
    private long stepPeriod = DEFAULT_STEP_PERIOD_MS;

    // Auction state
    private enum AuctionState {IDLE, COLLECTING_BIDS}

    private AuctionState auctionState = AuctionState.IDLE;
    private MeetingRequest currentRequest;
    private String currentRequesterName;
    private Map<String, RoomBid> receivedBids = new LinkedHashMap<>();
    private Set<String> expectedRoomAgents = new LinkedHashSet<>();
    private Set<String> rfpSentTo = new LinkedHashSet<>();

    private long bidTimeout = DEFAULT_BID_TIMEOUT;
    private long rfpRetryInterval = DEFAULT_RFP_RETRY_INTERVAL;

    /** Steps after which a won reservation is released. */
    private int releaseAfterSteps = 15;

    // Queue of pending booking requests from PersonAgents
    private Queue<AgentWave> pendingBookingRequests = new LinkedList<>();

    // Simulation reference, used only to discover rooms when no roomTargets are configured
    private Simulation simulation;
    private String nodeId = "unknown";
    private List<String> roomTargets = new ArrayList<>();

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
        if (configuration.containsKey("stepPeriod"))
            stepPeriod = Long.parseLong(configuration.getAValue("stepPeriod"));
        releaseAfterSteps = readInt(configuration, "releaseAfterSteps", releaseAfterSteps);
        if (configuration.containsKey("nodeId"))
            nodeId = configuration.getAValue("nodeId");
        if (configuration.containsKey("roomTargets"))
            roomTargets = new ArrayList<>(java.util.Arrays.asList(
                    configuration.getAValue("roomTargets").split(",")));
        roomTargets.removeIf(String::isEmpty);
        if (configuration.containsKey("bidTimeout"))
            bidTimeout = Long.parseLong(configuration.getAValue("bidTimeout"));
        if (configuration.containsKey("rfpRetryInterval"))
            rfpRetryInterval = Long.parseLong(configuration.getAValue("rfpRetryInterval"));
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        e.addGeneralContext(context);
        return super.addGeneralContext(context);
    }

    /**
     * Receives and processes events right away: booking requests are queued, bids are recorded and, when the last
     * expected bid arrives or when the auction times out, the auction is resolved.
     */
    @Override
    public synchronized boolean postAgentEvent(AgentEvent event) {
        if (event.getType() != AgentEvent.AgentEventType.AGENT_WAVE || !(event instanceof AgentWave))
            return false;
        AgentWave wave = (AgentWave) event;
        try {
            SmartMeetingMessageType type = SmartMeetingMessageCodec.decodeType(wave);
            switch (type) {
                case BOOKING_REQUEST:
                    pendingBookingRequests.add(wave);
                    break;
                case BID:
                    handleBid(wave);
                    break;
                case AUCTION_TIMEOUT:
                    handleAuctionTimeout(SmartMeetingMessageCodec.decodeRequestId(wave));
                    break;
                case RFP_RETRY:
                    handleRfpRetry(SmartMeetingMessageCodec.decodeRequestId(wave));
                    break;
                case RELEASE_DUE:
                    releaseReservation(SmartMeetingMessageCodec.decodeRoomAgentName(wave),
                            SmartMeetingMessageCodec.decodeReservationId(wave));
                    break;
                default:
                    break;
            }
        } catch (IllegalArgumentException ignored) {
            // Message belongs to another scenario.
        }
        return true;
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        if (ENVIRONMENT.equals(designation))
            return e;
        return AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)
                .equals(designation) ? e.getMessagingShard() : null;
    }

    @Override
    public boolean startSuspended() {
        if (!super.start())
            return false;
        if (e.getMessagingShard() != null)
            e.getMessagingShard().signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        return true;
    }

    @Override
    public boolean start() {
        return startSuspended() && e.startStepping(this::step, stepPeriod);
    }

    @Override
    public boolean stop() {
        e.stopStepping();
        return super.stop();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public synchronized void step() {
        currentStep++;
        if (auctionState == AuctionState.IDLE)
            startNextAuction();
    }

    private void startNextAuction() {
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
        expectedRoomAgents = discoverExpectedRoomAgents();
        rfpSentTo.clear();
        auctionStartedStep = currentStep;
        auctionState = AuctionState.COLLECTING_BIDS;
        broadcastRFP(currentRequest);
        ScenarioTrace.record(getEntityName(), "AuctionAgent", "auction-started",
                currentRequest.getRequestId(), null, null, null, nodeId);
        // No room to ask: nothing to wait for.
        if (expectedRoomAgents.isEmpty()) {
            resolveAuction();
            return;
        }
        long now = e.getCurrentTime();
        String requestId = currentRequest.getRequestId();
        if (!e.schedule(now + bidTimeout, SmartMeetingMessageCodec.encodeAuctionTimeout(requestId)))
            lw("no temporal context: auction [] will only end when all rooms answer", requestId);
        e.schedule(now + rfpRetryInterval, SmartMeetingMessageCodec.encodeRfpRetry(requestId));
        li("started auction for [] from person [] (timeout at time [])", requestId, currentRequesterName,
                Long.valueOf(now + bidTimeout));
    }

    private void handleAuctionTimeout(String requestId) {
        if (auctionState != AuctionState.COLLECTING_BIDS || !currentRequest.getRequestId().equals(requestId))
            return;
        li("auction [] timed out with []/[] bids", requestId, Integer.valueOf(receivedBids.size()),
                Integer.valueOf(expectedRoomAgents.size()));
        ScenarioTrace.record(getEntityName(), "AuctionAgent", "auction-timeout", requestId, null, Boolean.FALSE,
                "bids:" + receivedBids.size() + "/" + expectedRoomAgents.size(), nodeId);
        resolveAuction();
    }

    private void handleRfpRetry(String requestId) {
        if (auctionState != AuctionState.COLLECTING_BIDS || !currentRequest.getRequestId().equals(requestId))
            return;
        retryMissingRfps();
        e.schedule(e.getCurrentTime() + rfpRetryInterval, SmartMeetingMessageCodec.encodeRfpRetry(requestId));
    }

    /** Resends the RFP to expected rooms that have not answered yet (covers lost/early messages). */
    private void retryMissingRfps() {
        for (String room : expectedRoomAgents) {
            if (receivedBids.containsKey(room))
                continue;
            boolean sent = e.sendTo(room, SmartMeetingMessageCodec.encodeRequestForProposals(currentRequest));
            ScenarioTrace.record(getEntityName(), "AuctionAgent", "rfp-resent",
                    currentRequest.getRequestId(), room, Boolean.valueOf(sent),
                    sent ? null : "delivery-failed", nodeId);
        }
    }

    private void handleBid(AgentWave wave) {
        RoomBid bid = SmartMeetingMessageCodec.decodeRoomBid(wave);
        // Bids for an auction that is not running anymore (e.g. after its timeout) are ignored.
        if (auctionState != AuctionState.COLLECTING_BIDS || currentRequest == null
                || !currentRequest.getRequestId().equals(bid.getRequestId()))
            return;
        if (!expectedRoomAgents.contains(bid.getRoomAgentName())) {
            ScenarioTrace.record(getEntityName(), "AuctionAgent", "bid-rejected",
                    bid.getRequestId(), bid.getRoomId(), Boolean.FALSE,
                    "sender-not-in-auction-participants:" + bid.getRoomAgentName(), nodeId);
            return;
        }
        if (receivedBids.containsKey(bid.getRoomAgentName()))
            return; // duplicate bid (room answered an RFP retry twice)
        receivedBids.put(bid.getRoomAgentName(), bid);
        ScenarioTrace.record(getEntityName(), "AuctionAgent", "bid-received",
                bid.getRequestId(), bid.getRoomId(), Boolean.valueOf(bid.isFeasible()), bid.getReason(), nodeId);
        // The last expected bid arrived: resolve now.
        if (receivedBids.keySet().containsAll(expectedRoomAgents))
            resolveAuction();
    }

    private void broadcastRFP(MeetingRequest request) {
        for (String room : expectedRoomAgents) {
            boolean sent = e.sendTo(room, SmartMeetingMessageCodec.encodeRequestForProposals(request));
            if (sent)
                rfpSentTo.add(room);
            ScenarioTrace.record(getEntityName(), "AuctionAgent", "rfp-sent",
                    request.getRequestId(), room, Boolean.valueOf(sent),
                    sent ? null : "delivery-failed", nodeId);
        }
    }

    private Set<String> discoverExpectedRoomAgents() {
        Set<String> expected = new LinkedHashSet<>();
        if (!roomTargets.isEmpty()) {
            expected.addAll(roomTargets);
            return expected;
        }
        if (simulation != null)
            for (Entity<?> entity : simulation.getSimulationObjects())
                if (entity instanceof RoomAgent)
                    expected.add(entity.asContext().getEntityName());
        return expected;
    }

    private void resolveAuction() {
        List<RoomBid> bids = new ArrayList<>(receivedBids.values());
        RoomBid winner = selectBestBid(bids);

        int feasibleCount = 0;
        for (RoomBid bid : bids)
            if (bid.isFeasible()) feasibleCount++;

        if (winner != null) {
            e.sendTo(winner.getRoomAgentName(), SmartMeetingMessageCodec.encodeAcceptBid(winner));
            String reservationId = "RES-" + winner.getRoomId() + "-" + winner.getRequestId();
            if (!e.schedule(e.getCurrentTime() + releaseAfterSteps,
                    SmartMeetingMessageCodec.encodeReleaseDue(winner.getRoomAgentName(), reservationId)))
                lw("no temporal context: reservation [] will not be released", reservationId);

            // Reject losing bids
            for (RoomBid bid : bids) {
                if (bid.getRoomAgentName().equals(winner.getRoomAgentName()))
                    continue;
                e.sendTo(bid.getRoomAgentName(),
                        SmartMeetingMessageCodec.encodeRejectBid(bid.getRequestId()));
            }

            // Respond to person (direct, 1 step)
            e.sendDirect(currentRequesterName, SmartMeetingMessageCodec.encodeBookingResponse(
                    currentRequest.getRequestId(), true, winner.getRoomId(), null));

            li("auction [] WON by room [] (score []) from [] bids",
                    currentRequest.getRequestId(), winner.getRoomId(),
                    Integer.valueOf(winner.getScore()), Integer.valueOf(bids.size()));
            ScenarioTrace.record(getEntityName(), "AuctionAgent", "auction-resolved",
                    currentRequest.getRequestId(), winner.getRoomId(), Boolean.TRUE, null, nodeId);
            winsPerRoom.merge(winner.getRoomId(), 1, Integer::sum);
            outcomes.add(new AuctionOutcome(currentRequest.getRequestId(), currentRequesterName,
                    auctionStartedStep, currentStep, true, winner.getRoomId(), winner.getScore(),
                    bids.size(), feasibleCount, null));
        } else {
            e.sendDirect(currentRequesterName, SmartMeetingMessageCodec.encodeBookingResponse(
                    currentRequest.getRequestId(), false, null, "no feasible room"));
            li("auction [] FAILED — no feasible bid from [] responses",
                    currentRequest.getRequestId(), Integer.valueOf(bids.size()));
            ScenarioTrace.record(getEntityName(), "AuctionAgent", "auction-resolved",
                    currentRequest.getRequestId(), null, Boolean.FALSE, "no feasible room", nodeId);
            outcomes.add(new AuctionOutcome(currentRequest.getRequestId(), currentRequesterName,
                    auctionStartedStep, currentStep, false, null, 0, bids.size(), feasibleCount,
                    "no feasible room"));
        }

        currentRequest = null;
        currentRequesterName = null;
        auctionStartedStep = -1;
        auctionState = AuctionState.IDLE;
        // The run may end at any time relative to other nodes, so persist the trace now.
        ScenarioTrace.exportRun();
    }

    /**
     * Selects the highest-scoring feasible bid. When two or more bids tie on score, the winner is the room agent with
     * the smallest name, so that the result does not depend on the order in which bids arrived.
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
        return Collections.min(bestTier, Comparator.comparing(RoomBid::getRoomAgentName));
    }

    public List<AuctionOutcome> getOutcomes() {
        return Collections.unmodifiableList(outcomes);
    }

    public Map<String, Integer> getWinsPerRoom() {
        return Collections.unmodifiableMap(winsPerRoom);
    }

    private void releaseReservation(String roomAgentName, String reservationId) {
        if (e.sendTo(roomAgentName, SmartMeetingMessageCodec.encodeReleaseRoom(reservationId)))
            li("released reservation [] for room []", reservationId, roomAgentName);
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
