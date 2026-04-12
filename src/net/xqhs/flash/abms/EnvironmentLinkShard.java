package net.xqhs.flash.abms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.xqhs.flash.abms.AgentManagementContext.AgentManagementActionData;
import net.xqhs.flash.abms.SimulationContext.ActionRecord;
import net.xqhs.flash.abms.SimulationContext.BaseContext.BaseActionData;
import net.xqhs.flash.abms.communication.ProximityCommunicationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.SpaceContext.SpaceActionData;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiValueMap;


public class EnvironmentLinkShard extends AgentShardCore {

    protected static final String SHARD_NAME = "Environment";

    SpaceContext space = null;
    ProximityCommunicationContext proximityCommunication = null;
    WaveReceiver waveInbox = null;
    List<AgentWave> receivedWaves = new ArrayList<>();
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
        else if (context instanceof ProximityCommunicationContext) {
            proximityCommunication = (ProximityCommunicationContext) context;
            waveInbox = wave -> receivedWaves.add(wave);
        }
        if (!super.addGeneralContext(context))
            return false;

        if (proximityCommunication != null && getContext() != null && waveInbox != null)
            proximityCommunication.register(getContext(), waveInbox);

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

    public Set<Position> getFreeNeighborPositions(Position pos) {
        return space.getFreeNeighborPositions(pos);
    }

    public Set<Position> getPassableNeighborPositions(Position pos, Predicate<EntityProxy<?>> isPassable) {
        return space.getPassableNeighborPositions(pos, isPassable);
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

    public boolean isAlive() {
        return agentManagement == null || !agentManagement.isMarkedForDestruction(getContext());
    }

    public boolean isTargetAlive(EntityProxy<?> target) {
        return agentManagement == null || !agentManagement.isMarkedForDestruction(target);
    }

    public boolean requestDestroyAgent(EntityProxy<?> target) {
        return agentManagement.addPendingAction(new ActionRecord(getContext(),
                new MultiValueMap()
                        .add(BaseActionData.ACTION.s(), AgentManagementActionData.DESTROY_ACTION.s())
                        .addObject(AgentManagementActionData.DESTROY_TARGET.s(), target)));
    }

    public void notifyAgentDestroyed() {
        getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
    }

    public boolean broadcast(AgentWave wave) {
        if (proximityCommunication == null)
            return false;
        return proximityCommunication.broadcast(getContext(), wave);
    }

    public List<AgentWave> clearWaves() {
        if (receivedWaves.isEmpty())
            return Collections.emptyList();
        List<AgentWave> result = new ArrayList<>(receivedWaves);
        receivedWaves.clear();
        return result;
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
