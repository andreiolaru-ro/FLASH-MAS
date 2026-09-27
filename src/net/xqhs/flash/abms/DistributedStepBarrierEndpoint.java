package net.xqhs.flash.abms;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * WebSocket endpoint used by distributed executors to synchronize logical steps.
 */
public class DistributedStepBarrierEndpoint extends BaseAgent implements ShardContainer, WaveReceiver {
    private static final String DONE = "STEP_DONE";
    private static final String RELEASE = "STEP_RELEASE";
    private static final String STEP = "step";

    private final Set<String> expectedNodes = new LinkedHashSet<>();
    private final Set<String> completedNodes = new HashSet<>();
    private String coordinator;
    private String nodeId;
    private MessagingShard messaging;
    private long releasedStep = -1;

    public DistributedStepBarrierEndpoint(String nodeId, String coordinator, String allNodes) {
        this.nodeId = nodeId;
        this.coordinator = coordinator;
        if (allNodes != null && !allNodes.isEmpty())
            for (String id : Arrays.asList(allNodes.split(",")))
                expectedNodes.add("stepbarrier-" + id);
        expectedNodes.removeIf(String::isEmpty);
        if (expectedNodes.isEmpty())
            expectedNodes.add(nodeId);
        MultiTreeMap config = new MultiTreeMap();
        config.addFirstValue("name", "stepbarrier-" + nodeId);
        configure(config);
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends net.xqhs.flash.core.Entity<?>> context) {
        if (context instanceof PylonProxy && messaging == null) {
            messaging = (MessagingShard) AgentShardCore.instantiateRecommendedShard(
                    AgentShardDesignation.StandardAgentShard.MESSAGING, (PylonProxy) context, null, this);
            if (context instanceof WaveMessagingPylonProxy)
                ((WaveMessagingPylonProxy) context).register(getEntityName(), this);
        }
        return super.addGeneralContext(context);
    }

    @Override
    public void receive(AgentWave wave) {
        postAgentEvent(wave);
    }

    public void announceStep(long step) {
        AgentWave wave = new AgentWave(DONE);
        wave.add(STEP, String.valueOf(step));
        wave.add("node", nodeId);
        send(coordinator, wave);
    }

    public void awaitRelease(long step) {
        synchronized (completedNodes) {
            while (releasedStep < step) {
                try {
                    completedNodes.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for distributed step release", e);
                }
            }
        }
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        if (!(event instanceof AgentWave))
            return false;
        AgentWave wave = (AgentWave) event;
        if (DONE.equals(wave.getContent()) && getEntityName().equals(coordinator)) {
            int step = parseStep(wave);
            synchronized (completedNodes) {
                if (step == releasedStep + 1) {
                    completedNodes.add("stepbarrier-" + wave.get("node"));
                    if (completedNodes.containsAll(expectedNodes)) {
                        completedNodes.clear();
                        releasedStep = step;
                        for (String target : expectedNodes) {
                            AgentWave release = new AgentWave(RELEASE);
                            release.add(STEP, String.valueOf(step));
                            send(target, release);
                        }
                        completedNodes.notifyAll();
                    }
                }
            }
            return true;
        }
        if (RELEASE.equals(wave.getContent())) {
            synchronized (completedNodes) {
                releasedStep = Math.max(releasedStep, parseStep(wave));
                completedNodes.notifyAll();
            }
            return true;
        }
        return false;
    }

    private int parseStep(AgentWave wave) {
        return Integer.parseInt(wave.get(STEP));
    }

    private void send(String target, AgentWave wave) {
        if (messaging == null)
            throw new IllegalStateException("Distributed step barrier messaging is unavailable");
        for (int attempt = 0; attempt < 100; attempt++) {
            messaging.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
            if (messaging.sendMessage(messaging.getAgentAddress(), target, wave.getSerializedContent()))
                return;
            try {
                Thread.sleep(20L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while sending distributed step barrier message", e);
            }
        }
        throw new IllegalStateException("Unable to send distributed step barrier message to " + target);
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        return AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)
                .equals(designation) ? messaging : null;
    }

    @Override
    public String getEntityName() {
        return getName();
    }

    @Override
    public boolean start() {
        if (!super.start())
            return false;
        if (messaging != null)
            messaging.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        return true;
    }

    @Override
    public EntityProxy<net.xqhs.flash.core.agent.Agent> asContext() {
        return this;
    }

}
