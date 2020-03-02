package net.xqhs.flash.core.shard;

import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;

/**
 * The class extends {@link AgentShardCore} and offers additional functionality for {@link AgentShard} implementations,
 * especially in working together with other (known types of) shards.
 * 
 * @author Andrei Olaru
 */
public class AgentShardGeneral extends AgentShardCore
{
	/**
	 * The class UID.
	 */
	private static final long serialVersionUID = 5742900133840920226L;
	
	/**
	 * @see AgentShardCore#AgentShardCore(AgentShardDesignation)
	 */
	protected AgentShardGeneral(AgentShardDesignation designation)
	{
		super(designation);
	}
	
	/**
	 * Relay for {@link ShardContainer#getAgentShard(AgentShardDesignation)}.
	 * 
	 * @param designation
	 *                        - the designation of the desired {@link AgentShard}.
	 * @return the {@link AgentShard} instance, if any was found; <code>null</code> otherwise.
	 */
	final protected AgentShard getAgentShard(AgentShardDesignation designation)
	{
		return getAgent().getAgentShard(designation);
	}
	
	/**
	 * Retrieves the agent's messaging shard. If a value is returned, it is usable (it implements
	 * {@link MessagingShard}). If no usable instance was found, an exception is thrown.
	 * 
	 * @return an instance of {@link MessagingShard}.
	 * @throws UnsupportedOperationException
	 *                                           - if there is no messaging shard or it does not implement
	 *                                           {@link MessagingShard}.
	 */
	protected MessagingShard getMessagingShard()
	{
		AgentShard msd = getAgentShard(StandardAgentShard.MESSAGING.toAgentShardDesignation());
		if(msd == null)
			throw new UnsupportedOperationException("Messaging shard is not present.");
		if(!(msd instanceof MessagingShard))
			throw new UnsupportedOperationException("Messaging shard is not of expected type.");
		return (MessagingShard) msd;
	}
	
	/**
	 * Uses the {@link MessagingShard} to send a message.
	 * 
	 * @param content
	 *                               - the content of the message.
	 * @param sourceEndpoint
	 *                               - the source endpoint of the message (as an internal endpoint, to which the address
	 *                               of the agent will be added).
	 * @param targetAgent
	 *                               - the name of the target agent.
	 * @param targetPathElements
	 *                               - internal elements in the target path.
	 * @return <code>true</code> if the message has been successfully sent (as reported by the {@link MessagingShard}.
	 * @throws UnsupportedOperationException
	 *                                           if no usable {@link MessagingShard} has been found.
	 */
	protected boolean sendMessage(String content, String sourceEndpoint, String targetAgent,
			String... targetPathElements)
	{
		MessagingShard msd = getMessagingShard();
		return msd.sendMessage(msd.makePath(targetAgent, targetPathElements), msd.makeLocalPath(sourceEndpoint),
				content);
	}
}
