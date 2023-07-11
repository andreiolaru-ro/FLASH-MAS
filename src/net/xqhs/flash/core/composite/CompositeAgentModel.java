package net.xqhs.flash.core.composite;

import net.xqhs.flash.core.RunnableEntity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.support.Pylon;

/**
 * Interface for any implementation that is similar to a {@link CompositeAgent}, in that it has shards.
 * 
 * @author Andrei Olaru
 */
public interface CompositeAgentModel extends Agent, RunnableEntity<Pylon>
{
	/**
	 * Adds a shard to the agent, which has been configured beforehand.
	 *
	 * @param shard
	 *            - the {@link AgentShard} instance to add.
	 * @return the agent instance itself. This can be used to continue adding other shards.
	 */
	CompositeAgentModel addShard(AgentShard shard);
}
