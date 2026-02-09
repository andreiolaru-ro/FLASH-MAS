package net.xqhs.flash.abms;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

interface Context {
	
}

class SpatialContext implements Context {
	
	protected Map<EntityProxy<?>, Position> entityPositions;
	protected Map<Position, Set<EntityProxy<?>>>	entityInPosition;
	protected Topology<Position>					topology;
	
	public Position getPosition(EntityProxy<?> entity) {
		return entityPositions.get(entity);
	}
	
	public Set<Position> getVicinity(Position pos) {
		return topology.getVicinity(pos);
	}
	
	public Set<Position> getFreeNeighborPositions(Position pos) {
		return getVicinity(pos).stream()
				.filter(p -> !entityInPosition.containsKey(p) || entityInPosition.get(p).isEmpty())
				.collect(Collectors.toSet());
	}
	
	// @Override
	// public <A> Set<A> getNeighbors(GridPosition pos, Function<GridPosition, A> agentAtPosition) {
	// Set<GridPosition> vicinity = getVicinity(pos);
	// Set<A> neighbors = new HashSet<>();
	// for (GridPosition neighborPos : vicinity) {
	// if (isValidPosition(neighborPos)) {
	// A agent = agentAtPosition.apply(neighborPos);
	// if (agent != null) {
	// neighbors.add(agent);
	// }
	// }
	// }
	// return neighbors;
	// }
	
}

class GridSpatialContext extends SpatialContext {
	GridTopology topology;
}

public class EnvironmentLinkShard extends AgentShardCore {
	
	protected static final String SHARD_NAME = "Environment";
	
	SpatialContext space = null;
	
	public EnvironmentLinkShard() {
		super(AgentShardDesignation.customShard(SHARD_NAME));
	}
	
	<T> T getContext(Class<T> cls) {
		for(Entity.EntityProxy<? extends Entity<?>> c : getFullContext())
			if(cls.isInstance(c))
				return cls.cast(c);
		return null;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(context instanceof SpatialContext)
			space = (SpatialContext) context;
		return super.addGeneralContext(context);
	}
	
	public Position getCurrentPosition() {
		return space.getPosition(getAgent());
	}
	
	public Set<Position> getVicinity(Position pos) {
		return space.getVicinity(pos);
	}
	
	public Set<Position> getFreeNeighborPositions(Position pos) {
		return space.getFreeNeighborPositions(pos);
	}
}
