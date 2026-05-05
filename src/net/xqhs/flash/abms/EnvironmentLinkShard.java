package net.xqhs.flash.abms;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.abms.AgentManagementContext.AgentManagementActionData;
import net.xqhs.flash.abms.SimulationContext.ActionRecord;
import net.xqhs.flash.abms.SimulationContext.BaseContext.BaseActionData;
import net.xqhs.flash.abms.communication.ProximityCommunicationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.SpaceContext.SpaceActionData;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiValueMap;


public class EnvironmentLinkShard extends AgentShardCore {

    protected static final String SHARD_NAME = "Environment";

    SpaceContext space = null;
    ProximityCommunicationContext proximityCommunication = null;
    AgentManagementContext agentManagement = null;
    RandomContext randomContext = null;

    public EnvironmentLinkShard() {
        super(AgentShardDesignation.customShard(SHARD_NAME));
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof SpaceContext)
            space = (SpaceContext) context;
        else if (context instanceof AgentManagementContext)
            agentManagement = (AgentManagementContext) context;
        else if (context instanceof RandomContext)
            randomContext = (RandomContext) context;
        else if (context instanceof ProximityCommunicationContext)
            proximityCommunication = (ProximityCommunicationContext) context;
        if (!super.addGeneralContext(context))
            return false;

        if (agentManagement != null && getContext() != null)
            agentManagement.registerAgent(getContext(), this);
        // FIXME should actually be *closest* context
        return true;
    }

    public Position getCurrentPosition() {
        return space.getPosition(getContext());
    }

    public Set<Position> getVicinity(Position pos) {
        return space.getVicinity(pos);
    }

    public Set<Position> getValidNeighborPositions(Position pos) {
        return space.getValidNeighborPositions(pos);
    }

    public boolean moveToPosition(Position target) {
        return space.addPendingAction(new ActionRecord(getContext(),
                new MultiValueMap()
                        .add(BaseActionData.ACTION.s(), SpaceActionData.MOVE_ACTION.s())
                        .addObject(SpaceActionData.MOVE_TARGET.s(), target)));
    }

    public Set<EntityProxy<?>> getEntitiesAt(Position pos) {
        return space.getEntitiesAt(pos);
    }

    public Map<Position, Set<EntityProxy<?>>> observe(int range) {
        return space.getEntitiesWithinRange(getCurrentPosition(), range);
    }

    public Topology<? extends Position> getTopology() {
        return space.getTopology();
    }

    public boolean requestDestroyAgent(EntityProxy<?> target) {
        return agentManagement.addPendingAction(new ActionRecord(getContext(),
                new MultiValueMap()
                        .add(BaseActionData.ACTION.s(), AgentManagementActionData.DESTROY_ACTION.s())
                        .addObject(AgentManagementActionData.DESTROY_TARGET.s(), target)));
    }

    public void notifyAgentDestroyed() {
        // no-op: destruction is now handled via events and self-deregistration
    }

    public boolean broadcast(AgentWave wave) {
        if (proximityCommunication == null)
            return false;
        return proximityCommunication.broadcast(getContext(), wave);
    }

    public boolean sendWaveTo(EntityProxy<?> target, AgentWave wave) {
        if (proximityCommunication == null)
            return false;
        return proximityCommunication.sendWaveTo(target, wave);
    }

    @SuppressWarnings("unchecked")
    public Set<EntityProxy<?>> getEntitiesInVicinity() {
        if (space == null || getContext() == null)
            return Collections.emptySet();
        Position pos = getCurrentPosition();
        if (pos == null)
            return Collections.emptySet();
        java.util.HashSet<EntityProxy<?>> result = new java.util.HashSet<>();
        for (Position vpos : (Set<Position>) space.getVicinity(pos))
            result.addAll(space.getEntitiesAt(vpos));
        return result;
    }

    public int nextInt(int bound) {
        return randomContext.nextInt(bound);
    }

    public double nextDouble() {
        return randomContext.nextDouble();
    }

    public boolean nextBoolean() {
        return randomContext.nextBoolean();
    }

    public double nextGaussian() {
        return randomContext.nextGaussian();
    }
}
