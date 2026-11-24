package Tiberiu.smartGrid.hybrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.AgentWave;

/**
 * Native FLASH-MAS context bridging the infrastructure layer ({@link HybridPylon})
 * and the control layer ({@link HybridSimulationExecutor}) to report behavioral
 * divergences and coordinate dynamic bubble promotion/demotion.
 */
public class BubbleContext extends EntityCore<Entity<?>> implements EntityProxy<BubbleContext> {

    public static class DivergenceRecord {
        public final String		sourceAgent;
        public final String		targetMockedAgent;
        public final long		tick;
        public final AgentWave	triggeringWave;

        public DivergenceRecord(String sourceAgent, String targetMockedAgent, long tick, AgentWave triggeringWave) {
            this.sourceAgent = sourceAgent;
            this.targetMockedAgent = targetMockedAgent;
            this.tick = tick;
            this.triggeringWave = triggeringWave;
        }
    }

    private final List<DivergenceRecord> pendingDivergences = new ArrayList<>();

    public BubbleContext(String name) {
        this.name = name;
    }

    /**
     * Invoked by {@link HybridPylon} when an outbound wave from a LIVE agent
     * deviates from the recorded baseline trace for a MOCKED destination.
     */
    public void reportDivergence(String sourceAgent, String targetMockedAgent, long tick, AgentWave actualWave) {
        pendingDivergences.add(new DivergenceRecord(sourceAgent, targetMockedAgent, tick, actualWave));
    }

    /**
     * Drains and returns all divergence records accumulated during the current tick.
     */
    public List<DivergenceRecord> drainDivergences() {
        if (pendingDivergences.isEmpty()) {
            return Collections.emptyList();
        }
        List<DivergenceRecord> drained = new ArrayList<>(pendingDivergences);
        pendingDivergences.clear();
        return drained;
    }

    @Override
    public String getEntityName() {
        return name != null ? name : "BubbleContext";
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Entity<?>>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }
}