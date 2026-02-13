package net.xqhs.flash.abms.space;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.SimulationContext.BaseContext;
import net.xqhs.flash.abms.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;


public class SpaceContext extends BaseContext implements SimulationContext, EntityProxy<SpaceContext> {
	
	public enum SpaceActionData implements ActionData {
		MOVE_ACTION, MOVE_TARGET,
		
		;
		
		@Override
		public String s() {
			return this.toString();
		}
	}
	
	protected Map<EntityProxy<?>, Position>			entityPositions;
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
	
	@Override
	public void validateAndExecutependingActions() {
		
		for(ActionRecord a : pendingActions) {
			EntityProxy<?> e = a.getEntity();
			if(SpaceActionData.MOVE_ACTION.s().equals(a.getActionData().get(BaseActionData.ACTION.s()))) {
				Position currentPosition = entityPositions.get(e);
				Position targetPosition = (Position) a.getActionData().getObject(SpaceActionData.MOVE_TARGET.s());
				if(currentPosition == null)
					le("no position found for", e.getEntityName());
				else if(!a.getActionData().containsKey(SpaceActionData.MOVE_TARGET.s())
						|| !topology.isValidPosition(targetPosition))
					le("New position [] invalid for []", targetPosition, e.getEntityName());
				else {
					entityInPosition.get(currentPosition).remove(e);
					entityInPosition.get(targetPosition).add(e);
					entityPositions.put(e, targetPosition);
				}
			}
			else {
				le("Invalid action", a.getActionData().get(BaseActionData.ACTION.s()));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <C extends Entity<Simulation>> EntityProxy<C> asContext() {
		return (EntityProxy<C>) this;
	}
	
	@Override
	public String getEntityName() {
		// TODO Auto-generated method stub
		return null;
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