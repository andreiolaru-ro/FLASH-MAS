package net.xqhs.flash.abms;

import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.ActionRecord.ActionStatus;
import net.xqhs.flash.abms.BaseContext.BaseActionData;
import net.xqhs.flash.abms.SpatialContext.SpaceActionData;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiValueMap;

class ActionRecord {
	enum ActionStatus {
		INITIALIZED, PLANNED, PENDING, FAILED, COMPLETED,
	}
	
	ActionStatus	status;
	EntityProxy<?>	entity;
	MultiValueMap	actionData				= null;
	MultiValueMap	completionInformation	= null;
	
	public ActionRecord(EntityProxy<?> e, MultiValueMap data) {
		status = ActionStatus.INITIALIZED;
		entity = e;
		actionData = (MultiValueMap) data.lock();
	}
	
	public void setStatus(ActionStatus status, MultiValueMap information) {
		this.status = status;
		if(information != null)
			this.completionInformation = information;
	}
	
	public ActionStatus getStatus() {
		return status;
	}
	
	public MultiValueMap getCompletionInformation() {
		return completionInformation;
	}
	
	public MultiValueMap getActionData() {
		return actionData;
	}
}

interface Context {
	public interface ActionData {
		String s();
	}
	
	boolean addPendingAction(ActionRecord action);
}

abstract class BaseContext implements Context {
	public enum BaseActionData implements ActionData {
		ACTION
		
		;
		
		@Override
		public String s() {
			return this.toString();
		}
	}
	
	Deque<ActionRecord> pendingActions;
	
	@Override
	public boolean addPendingAction(ActionRecord action) {
		if(action.getStatus() != ActionStatus.INITIALIZED)
			return false;
		pendingActions.add(action);
		action.setStatus(ActionStatus.PENDING, null);
		return true;
	}
	
	public abstract void validateAndExecutependingActions();
}

class SpatialContext extends BaseContext implements Context {
	
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
		// TODO Auto-generated method stub
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
		// FIXME should actually be *closest* context
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
	
	public boolean moveToPosition(Position target) {
		return space.addPendingAction(new ActionRecord(getAgent(),
				new MultiValueMap().add(BaseActionData.ACTION.s(), SpaceActionData.MOVE_ACTION.s())));
	}
}
