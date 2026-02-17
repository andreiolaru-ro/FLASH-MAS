package net.xqhs.flash.abms;

import java.util.Set;

import net.xqhs.flash.abms.SimulationContext.ActionRecord;
import net.xqhs.flash.abms.SimulationContext.BaseContext.BaseActionData;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.SpaceContext.SpaceActionData;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiValueMap;

class GridSpatialContext extends SpaceContext {
	GridTopology topology;
}

public class EnvironmentLinkShard extends AgentShardCore {
	
	protected static final String SHARD_NAME = "Environment";
	
	SpaceContext space = null;
	EntityProxy<?>	entityProxy	= null;

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
		if(context instanceof SpaceContext)
			space = (SpaceContext) context;
		else if(context instanceof SteppableEntity)
			entityProxy = context;
		return super.addGeneralContext(context);
		// FIXME should actually be *closest* context
	}

	public Position getCurrentPosition() {
		return space.getPosition(entityProxy);
	}
	
	public Set<Position> getVicinity(Position pos) {
		return space.getVicinity(pos);
	}
	
	public Set<Position> getFreeNeighborPositions(Position pos) {
		return space.getFreeNeighborPositions(pos);
	}
	
	public boolean moveToPosition(Position target) {
		return space.addPendingAction(new ActionRecord(entityProxy,
				new MultiValueMap()
					.add(BaseActionData.ACTION.s(), SpaceActionData.MOVE_ACTION.s())
					.addObject(SpaceActionData.MOVE_TARGET.s(), target)));
	}
}
