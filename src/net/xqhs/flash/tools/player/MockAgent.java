package net.xqhs.flash.tools.player;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.recorder.SimulationEvent;
import net.xqhs.flash.core.recorder.RecorderService;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;

/**
 * MockAgent - A lightweight proxy agent for historical playback.
 * <p>
 * In the Hybrid Simulation Architecture, "Live" agents are booted natively by Flash-MAS
 * using their real Java classes. Therefore, this MockAgent serves a single, specialized role:
 * it intercepts historical actions from the JSON log and projects them into the Pylon
 * (the communication bus) as if they were happening in real-time.
 * </p>
 */
public class MockAgent extends BaseAgent {

    /** The shard responsible for interacting with the Flash-MAS messaging infrastructure. */
    private MessagingShard msgShard;

    /**
     * Initializes the MockAgent within the Flash-MAS lifecycle.
     * Registers the agent with the MultiAgentReplayer for historical event injection.
     *
     * @return true if initialization was successful.
     */
    @Override
    public boolean start() {
        // Initialize base Flash-MAS agent components
        if (!super.start()) return false;

        // Instantiate the messaging shard to enable Pylon communication
        msgShard = (MessagingShard) AgentShardCore.instantiateRecommendedShard(
                StandardAgentShard.MESSAGING,
                (PylonProxy) getContext(),
                null,
                new BaseAgentProxy() {
                    @Override
                    public boolean postAgentEvent(AgentEvent event) {
                        // A Mock agent strictly follows history; it ignores real-time incoming
                        // messages to prevent divergence from the scripted JSON timeline.
                        return true;
                    }
                });

        // Register this instance with the central coordinator
        MultiAgentReplayer.registerAgentInstance(getName(), this);

        // Log the initialization in the hybrid dashboard
        RecorderService.record(getName(), "AGENT_START_HYBRID", "Initialized strictly as MOCK");

        // Signal the core that boot is complete
        msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));

        return true;
    }

    /**
     * Executes a single historical step dictated by the JSON playback log.
     * This method is invoked externally by the {@link MultiAgentReplayer}.
     *
     * @param ev The historical SimulationEvent to be replayed.
     */
    public void executeHistoricalStep(SimulationEvent ev) {
        /*
         * TODO (Integration Point):
         * Map the JSON Payload back into an AgentWave object and call msgShard.sendMessage(wave);
         * This allows the MockAgent to physically interact with Live agents via the Pylon.
         */

        // Currently logging the event to provide visibility in the standard output
        if (ev.getType().contains("WAVE_SENT") || ev.getType().contains("MESSAGE")) {
            li("Simulating historical message from " + getName() + " -> Pylon.");
        }
    }
}