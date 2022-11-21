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
package net.xqhs.flash.core.support;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;

/**
 * Interface for any shard offering messaging functionality to an entity.
 * <p>
 * A <i>message</i> is a type of <i>wave</i> (implemented through {@link AgentWave}, generally considered to convey
 * information between entities which <i>may execute</i> on different nodes or are more loosely coupled (e.g. agents, or
 * agents and artifacts), as opposed to entities which are always found on the same node and are more tightly coupled
 * (e.g. shards inside the same agent).
 * 
 * @author Andrei Olaru
 */
public interface MessagingShard extends AgentShard
{
	/**
	 * Sends a message to another agent, according to the specific implementation.
	 * 
	 * @param source
	 *                    - the source (complete) endpoint of the message.
	 * @param target
	 *                    - the target (complete) endpoint of the message.
	 * @param content
	 *                    - the content of the message.
	 * @return <code>true</code> if the message was sent successfully.
	 */
	public boolean sendMessage(String source, String target, String content);
	
	/**
	 * @return the address of this agent in the {@link Pylon} this shard is assigned to.
	 */
	public String getAgentAddress();

	/**
	 * This can be called by non-agent entities to register their messaging shard, in case they are unable (or it would
	 * not be practical) to use {@link #signalAgentEvent(AgentEvent)}.
	 * 
	 * @param entityName
	 *            - the name of entity to be registered
	 */
	public void register(String entityName);
}
