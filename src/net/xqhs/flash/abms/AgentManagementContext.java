package net.xqhs.flash.abms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.abms.SimulationContext.BaseContext;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;

public class AgentManagementContext extends BaseContext
        implements SimulationContext, EntityProxy<AgentManagementContext> {

    public enum AgentManagementActionData implements ActionData {
        DESTROY_ACTION, DESTROY_TARGET;

        @Override
        public String s() {
            return this.toString();
        }
    }

    protected Map<EntityProxy<?>, EnvironmentLinkShard> agentShards = new HashMap<>();
    protected Set<EntityProxy<?>> pendingDestruction = new HashSet<>();
    protected SpaceContext<?> space;
    protected Simulation simulation;

    public void registerAgent(EntityProxy<?> proxy, EnvironmentLinkShard shard) {
        agentShards.put(proxy, shard);
    }

    @Override
    public boolean addPendingAction(ActionRecord action) {
        if (AgentManagementActionData.DESTROY_ACTION.s()
                .equals(action.getActionData().get(BaseActionData.ACTION.s()))) {
            EntityProxy<?> target = (EntityProxy<?>) action.getActionData()
                    .getObject(AgentManagementActionData.DESTROY_TARGET.s());
            if (target != null)
                pendingDestruction.add(target);
        }
        return super.addPendingAction(action);
    }

    public boolean isMarkedForDestruction(EntityProxy<?> entity) {
        return pendingDestruction.contains(entity);
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

                EnvironmentLinkShard targetShard = agentShards.get(target);
                if (targetShard == null) {
                    li("target [] already destroyed, skipping", target.getEntityName());
                    continue;
                }
                targetShard.notifyAgentDestroyed();

                if (space != null)
                    space.removeEntity(target);

                if (simulation != null)
                    for (Entity<?> entity : simulation.getSimulationObjects())
                        if (entity.asContext() == target || entity == target) {
                            simulation.deregisterEntity(entity);
                            break;
                        }

                agentShards.remove(target);
            } else {
                le("Invalid action", action.getActionData().get(BaseActionData.ACTION.s()));
            }
        }
        pendingActions.clear();
        pendingDestruction.clear();
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
