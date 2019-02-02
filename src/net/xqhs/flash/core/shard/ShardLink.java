package net.xqhs.flash.core.shard;

import net.xqhs.flash.core.composite.AgentEvent;

/**
 * This class provides a bas for any (agent) class that wishes to integrate {@link AgentShardCore} instances.
 * <p>
 * This is necessary in order to be able to access the <code>signalAgentEvent</code> method in the shard, protecting it
 * from unwanted access.
 * 
 * @author andreiolaru
 */
public abstract class ShardLink
{
	/**
	 * Relay method for the <code>signalAgentEvent</code> method in {@link AgentShardCore}.
	 * 
	 * @param shard
	 *            - the {@link AgentShardCore} instance to which the event must be signaled.
	 * @param event
	 *            - the event to signal.
	 */
	@SuppressWarnings("static-method")
	protected void signalEventToShard(AgentShardCore shard, AgentEvent event)
	{
		shard.signalAgentEvent(event);
	}
}