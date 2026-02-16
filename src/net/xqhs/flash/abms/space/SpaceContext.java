package net.xqhs.flash.abms.space;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.SimulationContext.BaseContext;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Debug.DebugItem;

public class SpaceContext<P extends Position> extends BaseContext
		implements SimulationContext, EntityProxy<SpaceContext<P>> {
	
	enum ContextDebugItem implements DebugItem {
		DEBUG_ALL_ACTIONS(true),
		
		;
		
		private boolean activate;
		
		private ContextDebugItem(boolean activate) {
			this.activate = activate;
		}
		
		@Override
		public boolean toBool() {
			return activate;
		}
	}
	
	public enum SpaceActionData implements ActionData {
		MOVE_ACTION, MOVE_TARGET,
		
		;
		
		@Override
		public String s() {
			return this.toString();
		}
	}
	
	protected Map<EntityProxy<?>, P>		entityPositions		= new HashMap<>();
	protected Map<P, Set<EntityProxy<?>>>	entityInPosition	= new HashMap<>();
	protected Topology<P>					topology;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		// FIXME
		topology = (Topology<P>) new GridTopology(Integer.parseInt(configuration.getAValue("width")),
				Integer.parseInt(configuration.getAValue("height")));
		return true;
	}
	
	public boolean place(EntityProxy<?> entity, P pos) {
		if(!topology.isValidPosition(pos))
			return false;
		entityPositions.put(entity, pos);
		entityInPosition.computeIfAbsent(pos, p -> new java.util.HashSet<>()).add(entity);
		return true;
	}
	
	public P getPosition(EntityProxy<?> entity) {
		return entityPositions.get(entity);
	}
	
	public Set<P> getVicinity(P pos) {
		return topology.getVicinity(pos);
	}
	
	public Set<P> getFreeNeighborPositions(P pos) {
		return getVicinity(pos).stream()
				.filter(p -> !entityInPosition.containsKey(p) || entityInPosition.get(p).isEmpty())
				.collect(Collectors.toSet());
	}

	public Set<EntityProxy<?>> getEntitiesAt(P pos) {
		Set<EntityProxy<?>> entities = entityInPosition.get(pos);
		return entities != null ? entities : new java.util.HashSet<>();
	}
	
	@Override
	public void validateAndExecutePendingActions() {
		
		for(ActionRecord a : pendingActions) {
			EntityProxy<?> e = a.getEntity();
			if(SpaceActionData.MOVE_ACTION.s().equals(a.getActionData().get(BaseActionData.ACTION.s()))) {
				P currentPosition = entityPositions.get(e);
				@SuppressWarnings("unchecked")
				P targetPosition = (P) a.getActionData().getObject(SpaceActionData.MOVE_TARGET.s());
				if(currentPosition == null)
					le("no position found for", e.getEntityName());
				else if(!a.getActionData().containsKey(SpaceActionData.MOVE_TARGET.s())
						|| !topology.isValidPosition(targetPosition))
					le("New position [] invalid for []", targetPosition, e.getEntityName());
				else {
					dbg(ContextDebugItem.DEBUG_ALL_ACTIONS, "moving entity [] from [] to []", e.getEntityName(),
							currentPosition, targetPosition);
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
	
	public Topology<? extends Position> getTopology() {
		return topology;
	}

	@Override
	public String visualizeAsString() {
		if (topology == null) {
			return null;
		}
		return topology.visualize(entityInPosition);
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