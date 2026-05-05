package net.xqhs.flash.abms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.abms.SimulationContext.BaseContext;
import net.xqhs.flash.abms.space.SpaceContext;
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

    protected Map<EntityProxy<?>, EnvironmentLinkShard> agentShards = new HashMap<>();
    protected Set<EntityProxy<?>> pendingDestroyEvents = new HashSet<>();
    protected SpaceContext<?> space;
    protected Simulation simulation;

    public void registerAgent(EntityProxy<?> proxy, EnvironmentLinkShard shard) {
        agentShards.put(proxy, shard);
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof SpaceContext)
            space = (SpaceContext<?>) context;
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        return super.addGeneralContext(context);
    }

    @Override
    public void validateAndExecutePendingActions() {
        if (space == null && simulation != null)
            for (SimulationContext ctx : simulation.getSimulationContexts())
                if (ctx instanceof SpaceContext)
                    space = (SpaceContext<?>) ctx;

        for (ActionRecord action : pendingActions) {
            if (AgentManagementActionData.DESTROY_ACTION.s()
                    .equals(action.getActionData().get(BaseActionData.ACTION.s()))) {
                EntityProxy<?> target = (EntityProxy<?>) action.getActionData()
                        .getObject(AgentManagementActionData.DESTROY_TARGET.s());
                if (target == null) {
                    le("destroy action with null target");
                    continue;
                }

                if (!agentShards.containsKey(target)) {
                    li("target [] already destroyed, skipping", target.getEntityName());
                    continue;
                }

                // Remove from space immediately
                if (space != null)
                    space.removeEntity(target);

                // Queue destroy event for delivery via sendEvents
                pendingDestroyEvents.add(target);
                agentShards.remove(target);
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
