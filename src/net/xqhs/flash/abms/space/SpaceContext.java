package net.xqhs.flash.abms.space;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.SimulationContext.BaseContext;
import net.xqhs.flash.abms.space.graph.GraphTopology;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
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
	/** The entities in each position, for topologies other than grids. */
	protected Map<P, Set<EntityProxy<?>>>	entityInPosition	= new HashMap<>();
	protected Set<EntityProxy<?>>[]			gridCells;
	protected int							gridWidth;
	protected int							gridHeight;
	protected Topology<P>					topology;

	@SuppressWarnings("unchecked")
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		String topologyType = configuration.getAValue("topology");
		if ("graph".equals(topologyType))
			topology = (Topology<P>) new GraphTopology(configuration);
		else {
			gridWidth = Integer.parseInt(configuration.getAValue("width"));
			gridHeight = Integer.parseInt(configuration.getAValue("height"));
			topology = (Topology<P>) new GridTopology(gridWidth, gridHeight);
			gridCells = new Set[gridWidth * gridHeight];
		}
		return true;
	}

	protected Set<EntityProxy<?>> cell(P pos, boolean create) {
		if(gridCells == null)
			return create ? entityInPosition.computeIfAbsent(pos, p -> new HashSet<>())
					: entityInPosition.get(pos);
		if(!(pos instanceof GridPosition))
			return null;
		return gridCell(((GridPosition) pos).getX(), ((GridPosition) pos).getY(), create);
	}

	protected Set<EntityProxy<?>> gridCell(int x, int y, boolean create) {
		if(x < 0 || x >= gridWidth || y < 0 || y >= gridHeight)
			return null;
		int index = y * gridWidth + x;
		if(gridCells[index] == null && create)
			gridCells[index] = new HashSet<>();
		return gridCells[index];
	}

	public boolean place(EntityProxy<?> entity, P pos) {
		if(!topology.isValidPosition(pos))
			return false;
		entityPositions.put(entity, pos);
		cell(pos, true).add(entity);
		return true;
	}

	public P getPosition(EntityProxy<?> entity) {
		return entityPositions.get(entity);
	}

	public Set<P> getVicinity(P pos) {
		return topology.getVicinity(pos);
	}

    public Set<P> getVicinity(P pos, int range) {
        return topology.getVicinity(pos, range);
    }

	public Set<P> getValidNeighborPositions(P pos) {
		return getVicinity(pos).stream()
				.filter(p -> topology.isValidPosition(p))
				.collect(Collectors.toSet());
	}

	public void removeEntity(EntityProxy<?> entity) {
		P pos = entityPositions.remove(entity);
		Set<EntityProxy<?>> entities = pos == null ? null : cell(pos, false);
		if(entities != null)
			entities.remove(entity);
	}

	public Set<EntityProxy<?>> getEntitiesAt(P pos) {
		Set<EntityProxy<?>> entities = cell(pos, false);
		return entities != null ? entities : java.util.Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Map<P, Set<EntityProxy<?>>> getEntitiesWithinRange(P center, int range) {
		Map<P, Set<EntityProxy<?>>> result = new HashMap<>();
		if(gridCells != null && center instanceof GridPosition) {
			// the cells of the square around the center (without it), read directly from the grid
			int cx = ((GridPosition) center).getX(), cy = ((GridPosition) center).getY();
			for(int dx = -range; dx <= range; dx++)
				for(int dy = -range; dy <= range; dy++) {
					if(dx == 0 && dy == 0)
						continue;
					Set<EntityProxy<?>> entities = gridCell(cx + dx, cy + dy, false);
					if(entities != null && !entities.isEmpty())
						result.put((P) new GridPosition(cx + dx, cy + dy), entities);
				}
			return result;
		}
        Set<P> positions = getVicinity(center, range);
        for (P pos : positions) {
            Set<EntityProxy<?>> entities = getEntitiesAt(pos);

            if (entities != null && !entities.isEmpty())
                result.put(pos, entities);
        }
		return result;
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
					dbg(ContextDebugItem.DEBUG_ALL_ACTIONS, "skipping move for removed entity []", e.getEntityName());
				else if(!a.getActionData().containsKey(SpaceActionData.MOVE_TARGET.s())
						|| !topology.isValidPosition(targetPosition))
					le("New position [] invalid for []", targetPosition, e.getEntityName());
				else {
//                  TODO: Check if this is still needed or should be deleted
//					dbg(ContextDebugItem.DEBUG_ALL_ACTIONS, "moving entity [] from [] to []", e.getEntityName(),
//							currentPosition, targetPosition);
					cell(currentPosition, false).remove(e);
					cell(targetPosition, true).add(e);
					entityPositions.put(e, targetPosition);
				}
			}
			else {
				le("Invalid action", a.getActionData().get(BaseActionData.ACTION.s()));
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
		// TODO Auto-generated method stub
		return null;
	}

	public Set<EntityProxy<?>> getAllEntities() {
		return new HashSet<>(entityPositions.keySet());
	}

	public Topology<? extends Position> getTopology() {
		return topology;
	}

	@Override
	public String visualizeAsString() {
		if (topology == null) {
			return null;
		}
		if (gridCells == null)
			return topology.visualize(entityInPosition);
		Map<P, Set<EntityProxy<?>>> cells = new HashMap<>();
		for (Map.Entry<EntityProxy<?>, P> entry : entityPositions.entrySet())
			cells.computeIfAbsent(entry.getValue(), p -> new HashSet<>()).add(entry.getKey());
		return topology.visualize(cells);
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
