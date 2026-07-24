package net.xqhs.flash.core.agent;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;

/**
 * Base class for any agent, specifying that its context must be a {@link Pylon}. This version cannot be a context of
 * other entities, but extending classes can change that,
 */
public class BaseAgent extends EntityCore<Pylon> implements Agent {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -8256368668672575500L;
	
	@Override
	public boolean isMainContext(Object context) {
		return context instanceof PylonProxy;
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		return null;
	}
	
	/**
	 * A basic implementation for {@link ShardContainer}.
	 */
	public class BaseAgentProxy implements ShardContainer {
		@Override
		public String getEntityName() {
			return getName();
		}
		
		@Override
		public boolean postAgentEvent(AgentEvent event) {
			return false;
		}
		
		@Override
		public AgentShard getAgentShard(AgentShardDesignation designation) {
			return null;
		}
		
	}
}
