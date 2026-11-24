package Tiberiu.smartGrid.hybrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.recorder.RecorderService;

/**
 * External tick-based simulation executor managing the dynamic execution bubble.
 * <p>
 * Drives standard {@link Agent} instances synchronously through {@link HybridPylon},
 * consumes divergence notifications from {@link BubbleContext} to run an incremental catch-up
 * sub-executor when MOCKED agents are promoted to LIVE, and contracts the execution bubble
 * via {@link BehavioralRealignmentDetector} upon behavioral realignment.
 */
public class HybridSimulationExecutor {

    private final HybridPylon						pylon;
    private final Map<String, Agent>				allAgents			= new LinkedHashMap<>();
    private final Set<String>						pinnedLiveAgents	= new HashSet<>();
    private final TickTraceStore					traceStore;
    private final boolean							hybridEnabled;

    private BubbleContext							bubbleContext		= null;

    /** Evaluates behavioral realignment using 4-tick sliding window checkpointing and 99.0% embedding similarity. */
    private final BehavioralRealignmentDetector		realignmentDetector	=
            new BehavioralRealignmentDetector(6, 0.995);

    /** Tracks the latest logical tick up to which each agent's internal state is synchronized. */
    private final Map<String, Long>					lastSimulatedTick	= new HashMap<>();

    /** Telemetry metrics collected across the simulation timeline. */
    private final List<Integer>						liveCountHistory	= new ArrayList<>();
    private long									catchUpTimeNanos	= 0L;
    private long									totalStepsExecuted	= 0L;

    public HybridSimulationExecutor(HybridPylon pylon, TickTraceStore traceStore, boolean hybridEnabled) {
        this.pylon = pylon;
        this.traceStore = traceStore;
        this.hybridEnabled = hybridEnabled;
    }

    public void setBubbleContext(BubbleContext context) {
        this.bubbleContext = context;
    }

    public void registerAgent(Agent agent) {
        allAgents.put(agent.getName(), agent);
    }

    public void runSimulation(long maxTicks, Set<String> initialLiveAgents) {
        liveCountHistory.clear();
        lastSimulatedTick.clear();
        catchUpTimeNanos = 0L;
        totalStepsExecuted = 0L;

        if (hybridEnabled) {
            this.pinnedLiveAgents.addAll(initialLiveAgents);
            this.pylon.enableHybridMode(traceStore, initialLiveAgents);

            for (String agentName : initialLiveAgents) {
                Agent agent = allAgents.get(agentName);
                if (agent != null && !agent.isRunning()) {
                    agent.start();
                }
            }
        } else {
            for (Agent agent : allAgents.values()) {
                if (!agent.isRunning()) {
                    agent.start();
                }
            }
        }

        for (long tick = 0; tick < maxTicks; tick++) {
            RecorderService.setCurrentTick(tick);
            executeSingleTick(tick);
        }

        for (Agent agent : allAgents.values()) {
            if (agent.isRunning()) {
                agent.stop();
            }
        }
    }

    private void executeSingleTick(long tick) {
        // 1. Inject MOCKED -> LIVE messages from the historical trace
        pylon.onTickStart(tick);

        // 2. Advance only agents currently inside the LIVE bubble in deterministic registration order
        List<String> activeSnapshot = new ArrayList<>();
        for (String agentName : allAgents.keySet()) {
            if (!hybridEnabled || pylon.isLive(agentName)) {
                activeSnapshot.add(agentName);
            }
        }

        for (String agentName : activeSnapshot) {
            pylon.stepAgent(agentName, tick);
            lastSimulatedTick.put(agentName, tick);
            totalStepsExecuted++;
        }

        // 3. Drain divergence notifications from BubbleContext and process cascading promotions
        List<BubbleContext.DivergenceRecord> divergences =
                bubbleContext != null ? bubbleContext.drainDivergences() : Collections.emptyList();

        while (!divergences.isEmpty()) {
            BubbleContext.DivergenceRecord div = divergences.remove(0);
            if (!pylon.isLive(div.targetMockedAgent)) {
                long t0 = System.nanoTime();
                bringAgentUpToSpeed(div.targetMockedAgent, div.tick);
                catchUpTimeNanos += (System.nanoTime() - t0);

                pylon.promoteAgent(div.targetMockedAgent);
                realignmentDetector.resetAgent(div.targetMockedAgent);

                // Inject current-tick baseline messages from still-MOCKED senders (safe with Aggregator's per-sender map)
                for (TickTraceStore.RecordedMessage msg : traceStore.getIncomingMessagesForAgent(div.targetMockedAgent, tick)) {
                    if (!pylon.isLive(msg.source) && !msg.source.equals(div.sourceAgent)) {
                        AgentWave currentTickWave = new AgentWave(msg.content, div.targetMockedAgent)
                                .addSourceElements(msg.source);
                        pylon.deliverDirectToReceiver(div.targetMockedAgent, currentTickWave);
                    }
                }

                // Deliver the triggering divergent wave and advance the newly promoted agent at the current tick
                Agent promoted = allAgents.get(div.targetMockedAgent);
                if (promoted != null) {
                    pylon.deliver(div.triggeringWave);
                    if (lastSimulatedTick.getOrDefault(div.targetMockedAgent, -1L) < tick) {
                        pylon.stepAgent(div.targetMockedAgent, tick);
                        lastSimulatedTick.put(div.targetMockedAgent, tick);
                        totalStepsExecuted++;
                    }
                }
            }
            // Re-check BubbleContext in case the newly promoted agent triggered downstream divergences
            if (bubbleContext != null) {
                divergences.addAll(bubbleContext.drainDivergences());
            }
        }

        // 4. Evaluate bubble contraction (demotion from LIVE back to MOCKED via checkpointing + embeddings)
        if (hybridEnabled) {
            evaluateBubbleContraction(tick);
        }

        // 5. Record active bubble size at the end of the tick
        liveCountHistory.add(hybridEnabled ? pylon.getLiveAgents().size() : allAgents.size());
    }

    private void bringAgentUpToSpeed(String agentName, long targetTick) {
        Agent agent = allAgents.get(agentName);
        if (agent == null)
            return;

        if (!agent.isRunning()) {
            agent.start();
        }

        long startTick = lastSimulatedTick.getOrDefault(agentName, -1L) + 1L;
        for (long pastTick = startTick; pastTick < targetTick; pastTick++) {
            List<TickTraceStore.RecordedMessage> incoming =
                    traceStore.getIncomingMessagesForAgent(agentName, pastTick);
            for (TickTraceStore.RecordedMessage msg : incoming) {
                AgentWave wave = new AgentWave(msg.content, agentName).addSourceElements(msg.source);
                pylon.deliverDirectToReceiver(agentName, wave);
            }
            pylon.stepAgent(agentName, pastTick);
            lastSimulatedTick.put(agentName, pastTick);
            totalStepsExecuted++;
        }
    }

    private void evaluateBubbleContraction(long tick) {
        for (String agentName : new ArrayList<>(pylon.getLiveAgents())) {
            if (pinnedLiveAgents.contains(agentName) || agentName.startsWith("Aggregator_"))
                continue;

            List<String> actualPayloads = pylon.getOutboundPayloadsForAgent(agentName);
            if (realignmentDetector.recordAndCheckRealignment(agentName, tick, actualPayloads, traceStore)) {
                pylon.demoteAgent(agentName);
            }
        }
    }

    public List<Integer> getLiveCountHistory() {
        return Collections.unmodifiableList(liveCountHistory);
    }

    public double getCatchUpTimeMs() {
        return catchUpTimeNanos / 1_000_000.0;
    }

    public long getTotalStepsExecuted() {
        return totalStepsExecuted;
    }
}