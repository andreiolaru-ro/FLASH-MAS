/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
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
 * 
 * 
 * @author Andrei Olaru
 */
public interface MessagingShard extends AgentShard
{
	/**
	 * Sends a message to another agent, according to the specific implementation.
	 * 
	 * @param source
	 *            - the source (internal) endpoint of the message.
	 * @param target
	 *            - the target (complete) endpoint of the message.
	 * @param content
	 *            - the content of the message.
	 * @return <code>true</code> if the message was sent successfully.
	 */
	public boolean sendMessage(String source, String target, String content);
	
	/**
	 * The method creates a complete path by attaching the specified elements and placing slashes between them.
	 * <p>
	 * E.g. it produces targetAgent/element1/element2/element3
	 * 
	 * @param targetAgent
	 *                             - the name of the searched agent.
	 * @param internalElements
	 *                             - the elements in the internal path.
	 * @return the complete path/address.
	 */
	public String makePath(String targetAgent, String... internalElements);
	
	/**
	 * The method creates a complete path by attaching the specified elements to the address of this agent.
	 * <p>
	 * E.g. it produces thisAgent/element1/element2/element3
	 * 
	 * @param elements
	 *                     - the elements in the path.
	 * @return the complete path/address.
	 */
	public String makeLocalPath(String... elements);
}
