package Tiberiu.smartGrid.hybrid;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.local.LocalPylon.SimpleLocalMessaging;

/**
 * Custom messaging shard recommended by {@link HybridPylon}.
 * Bridges external tick signals into standard {@code BaseAgent} instances
 * without requiring custom interfaces on agent classes.
 */
public class HybridMessagingShard extends SimpleLocalMessaging {

    private static final long serialVersionUID = 1L;

    /** Wave content reserved for external tick advancement. */
    public static final String TICK_WAVE_CONTENT = "__HYBRID_TICK__";

    /**
     * Pushes an external tick trigger into the host agent via its standard {@code postAgentEvent} hook.
     */
    public void triggerStep(long tick) {
        if (getAgent() != null) {
            AgentWave tickWave = new AgentWave(TICK_WAVE_CONTENT + ":" + tick, getAgentAddress());
            getAgent().postAgentEvent(tickWave);
        }
    }
}