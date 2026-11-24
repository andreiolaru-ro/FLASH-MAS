package net.xqhs.flash.abms;

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.abms.SimulationContext.BaseContext;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.ShardContainer;

public class AgentManagementContext extends BaseContext
        implements SimulationContext, EntityProxy<AgentManagementContext> {

    public static final String DESTROY_WAVE_CONTENT = "DESTROY";

    public enum AgentManagementActionData implements ActionData {
        DESTROY_ACTION, DESTROY_TARGET;

        @Override
        public String s() {
            return this.toString();
        }
    }

    protected Set<EntityProxy<?>> pendingDestroyEvents = new HashSet<>();
    protected Simulation simulation;

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        return super.addGeneralContext(context);
    }

    @Override
    public void validateAndExecutePendingActions() {
        for (ActionRecord action : pendingActions) {
            if (AgentManagementActionData.DESTROY_ACTION.s()
                    .equals(action.getActionData().get(BaseActionData.ACTION.s()))) {
                EntityProxy<?> target = (EntityProxy<?>) action.getActionData()
                        .getObject(AgentManagementActionData.DESTROY_TARGET.s());
                if (target == null) {
                    le("destroy action with null target");
                    continue;
                }
                if (simulation == null || !simulation.getSimulationObjects().contains(target)
                        || pendingDestroyEvents.contains(target))
                    continue;
                simulation.notifyEntityRemoved(target);
                // Queue destroy event for delivery via sendEvents
                pendingDestroyEvents.add(target);
            } else {
                le("Invalid action", action.getActionData().get(BaseActionData.ACTION.s()));
            }
        }
        pendingActions.clear();
    }

    @Override
    public void sendEvents(Entity<?> entity) {
        if (pendingDestroyEvents.isEmpty())
            return;
        EntityProxy<?> proxy = entity.asContext();
        if (proxy == null)
            proxy = (entity instanceof EntityProxy<?>) ? (EntityProxy<?>) entity : null;
        if (proxy == null || !pendingDestroyEvents.contains(proxy))
            return;

        // Send DESTROY wave to the entity; it will self-deregister in postAgentEvent
        if (entity instanceof ShardContainer) {
            ((ShardContainer) entity).postAgentEvent(new AgentWave(DESTROY_WAVE_CONTENT));
            pendingDestroyEvents.remove(proxy);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Simulation>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public String getEntityName() {
        return name;
    }
}
