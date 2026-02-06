package net.xqhs.flash.abms;

import java.util.Map;

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
	
	public Position getPosition(EntityProxy<?> entity) {
		return entityPositions.get(entity);
	}
	
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
	
}
