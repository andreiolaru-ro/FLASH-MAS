/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.shard;

import net.xqhs.flash.core.agent.AgentWave;
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
	 * @param sourceInternalEndpoint
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
	protected boolean sendMessage(String content, String sourceInternalEndpoint, String targetAgent,
			String... targetPathElements)
	{
		MessagingShard msd = getMessagingShard();
		return msd.sendMessage(AgentWave.makePath(msd.getAgentAddress(), sourceInternalEndpoint),
				AgentWave.makePath(targetAgent, targetPathElements), content);
	}
	
	/**
	 * Uses the {@link MessagingShard} to send a message.
	 * <p>
	 * WARNING: the complete source and complete destination given by the {@link AgentWave} are used directly, without
	 * any checks.
	 *
	 * @param agentWave
	 *            - the {@link AgentWave} to send as a message.
	 * @return <code>true</code> if the message has been successfully sent (as reported by the {@link MessagingShard}.
	 * @throws UnsupportedOperationException
	 *             if no usable {@link MessagingShard} has been found.
	 */
	protected boolean sendMessage(AgentWave agentWave) {
		MessagingShard msd = getMessagingShard();
		return msd.sendMessage(agentWave.getCompleteSource(), agentWave.getCompleteDestination(),
				agentWave.getSerializedContent());
	}
}
