package net.xqhs.flash.abms;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.abms.SimulationContext.BaseContext;
import net.xqhs.flash.abms.SimulationContext.BaseContext.BaseActionData;
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
                if (targetShard != null)
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
