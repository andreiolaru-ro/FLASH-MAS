package net.xqhs.flash.core.shard;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;

/**
 * This interface should be implemented by any proxy to an agent which needs to use {@link AgentShard}s. It enables the
 * shards to interact with agent functionality such as posting events to agent or getting information about the agent
 * name.
 * 
 * @author Andrei Olaru
 */
public interface ShardContainer extends EntityProxy<Agent>
{
	/**
	 * Agent shards should call this method to signal to the agent a new event (e.g. and <i>wave</i>).
	 * 
	 * @param event
	 *                  - the event to be signaled to the agent.
	 */
	void postAgentEvent(AgentEvent event);
}
