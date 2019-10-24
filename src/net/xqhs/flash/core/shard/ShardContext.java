package net.xqhs.flash.core.shard;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;

/**
 * This interface should be implemented by any class that represents a proxy to container for shards (e.g. an agent).
 * <p>
 * It allows shards contained by the container to post events to the container (agent).
 * 
 * @author Andrei Olaru
 */
public interface ShardContext extends EntityProxy<Agent>
{
	/**
	 * The method should be called by a shard to post an agent event, via this proxy.
	 * 
	 * @param event
	 *            - the event to post.
	 */
	void postAgentEvent(AgentEvent event);
}
