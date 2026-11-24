package Tiberiu.smartGrid.hybrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.recorder.RecorderService;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalPylon;

/**
 * Support infrastructure pylon enabling transparent hybrid execution (LIVE vs. MOCKED)
 * for standard unmodified FLASH-MAS agents via {@link HybridMessagingShard}.
 */
public class HybridPylon extends LocalPylon {

    /** Numerical tolerance preventing floating-point micro-jitter from waking MOCKED zones. */
    private static final double					NUMERIC_TOLERANCE	= 0.002;

    private final Set<String>					liveAgents			= new HashSet<>();
    private final Map<String, List<String>>		outboundLogThisTick	= new HashMap<>();

    private BubbleContext						bubbleContext		= null;
    private TickTraceStore						traceStore			= null;
    private long								currentTick			= 0;
    private boolean								hybridMode			= false;

    @Override
    public boolean configure(MultiTreeMap configuration) {
        this.useThread = false;
        return super.configure(configuration);
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof BubbleContext) {
            this.bubbleContext = (BubbleContext) context;
        }
        return super.addGeneralContext(context);
    }

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardName) {
        if (shardName.equals(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING))) {
            return HybridMessagingShard.class.getName();
        }
        return super.getRecommendedShardImplementation(shardName);
    }

    public PylonProxy getPylonProxy() {
        return this.messagingProxy;
    }

    public void enableHybridMode(TickTraceStore traceStore, Set<String> initialLiveAgents) {
        this.traceStore = traceStore;
        this.liveAgents.clear();
        this.liveAgents.addAll(initialLiveAgents);
        this.hybridMode = true;
    }

    public void promoteAgent(String agentName) {
        liveAgents.add(agentName);
    }

    public void demoteAgent(String agentName) {
        liveAgents.remove(agentName);
    }

    public boolean isLive(String agentName) {
        return !hybridMode || liveAgents.contains(agentName);
    }

    public Set<String> getLiveAgents() {
        return Collections.unmodifiableSet(liveAgents);
    }

    public List<String> getOutboundPayloadsForAgent(String agentName) {
        return outboundLogThisTick.getOrDefault(agentName, Collections.emptyList());
    }

    /**
     * Advances a single agent by one logical tick by delivering a synthetic tick wave
     * directly into its registered {@link HybridMessagingShard} receiver.
     */
    public void stepAgent(String agentName, long tick) {
        AgentWave tickWave = new AgentWave(HybridMessagingShard.TICK_WAVE_CONTENT + ":" + tick, agentName)
                .addSourceElements(agentName);
        deliverDirectToReceiver(agentName, tickWave);
    }

    private WaveReceiver findReceiverForAgent(String agentName) {
        if (messageReceivers.containsKey(agentName)) {
            return messageReceivers.get(agentName);
        }
        for (Map.Entry<String, WaveReceiver> entry : messageReceivers.entrySet()) {
            if (agentName.equals(resolveAgentName(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void onTickStart(long tick) {
        this.currentTick = tick;
        this.outboundLogThisTick.clear();

        if (!hybridMode || traceStore == null)
            return;

        List<TickTraceStore.RecordedMessage> tickMessages = traceStore.getMessagesAtTick(tick);
        for (TickTraceStore.RecordedMessage msg : tickMessages) {
            boolean sourceIsMocked = !liveAgents.contains(msg.source);
            boolean destIsLive = liveAgents.contains(msg.destination);

            if (sourceIsMocked && destIsLive) {
                AgentWave syntheticWave = new AgentWave(msg.content, msg.destination)
                        .addSourceElements(msg.source);
                super.deliver(syntheticWave);
            }
        }
    }

    public void deliverDirectToReceiver(String destAgentName, AgentWave wave) {
        WaveReceiver receiver = findReceiverForAgent(destAgentName);
        if (receiver != null) {
            receiver.receive(wave);
        }
    }

    @Override
    public boolean send(AgentWave wave) {
        String rawSource = wave.getFirstSource();
        if (rawSource == null || rawSource.isEmpty()) {
            rawSource = wave.getCompleteSource();
        }
        String source = resolveAgentName(rawSource);
        String dest = resolveAgentName(wave.getFirstDestinationElement());

        if (!hybridMode && source != null && dest != null) {
            RecorderService.record(source, source, dest, wave.getContent());
        }

        return super.send(wave);
    }

    @Override
    protected boolean deliver(AgentWave wave) {
        if (!hybridMode || traceStore == null) {
            return super.deliver(wave);
        }

        String rawSource = wave.getFirstSource();
        if (rawSource == null || rawSource.isEmpty()) {
            rawSource = wave.getCompleteSource();
        }
        String source = resolveAgentName(rawSource);
        String dest = resolveAgentName(wave.getFirstDestinationElement());

        // Suppress outbound waves emitted by an agent undergoing isolated historical catch-up
        if (!liveAgents.contains(source)) {
            return true;
        }

        // Record actual outbound payload for behavioral realignment checkpointing
        outboundLogThisTick.computeIfAbsent(source, k -> new ArrayList<>())
                .add(wave.getContent());

        // Case 1: LIVE -> LIVE (standard delivery)
        if (liveAgents.contains(dest)) {
            return super.deliver(wave);
        }

        // Case 2: LIVE -> MOCKED (verify conformity against the historical baseline trace)
        Optional<TickTraceStore.RecordedMessage> recordedOpt = traceStore.findMessage(currentTick, source, dest);
        String actualContent = wave.getContent();

        boolean matchesRecording = recordedOpt.isPresent()
                && isBehaviorallyEquivalent(recordedOpt.get().content, actualContent);

        if (matchesRecording) {
            return true;
        } else {
            if (bubbleContext != null) {
                bubbleContext.reportDivergence(source, dest, currentTick, wave);
            }
            if (liveAgents.contains(dest)) {
                return super.deliver(wave);
            }
            return true;
        }
    }

    /**
     * Compares two structured pipe-delimited payloads (e.g., {@code CLEAR|0.251} vs {@code CLEAR|0.250})
     * using exact symbolic token matching and a bounded numerical deadband {@link #NUMERIC_TOLERANCE}.
     */
    private boolean isBehaviorallyEquivalent(String recorded, String actual) {
        if (Objects.equals(recorded, actual))
            return true;
        if (recorded == null || actual == null)
            return false;

        String[] recTokens = recorded.split("\\|");
        String[] actTokens = actual.split("\\|");
        if (recTokens.length != actTokens.length)
            return false;

        for (int i = 0; i < recTokens.length; i++) {
            if (recTokens[i].equals(actTokens[i]))
                continue;
            try {
                double v1 = Double.parseDouble(recTokens[i]);
                double v2 = Double.parseDouble(actTokens[i]);
                if (Math.abs(v1 - v2) > NUMERIC_TOLERANCE) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private String resolveAgentName(String rawAddress) {
        if (rawAddress == null)
            return null;
        String[] parts = rawAddress.split("/");
        for (String part : parts) {
            if (part.startsWith("Prosumer_") || part.startsWith("Aggregator_")) {
                return part;
            }
        }
        return parts[parts.length - 1];
    }
}