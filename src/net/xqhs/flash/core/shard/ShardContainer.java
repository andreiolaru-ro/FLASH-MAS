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
	 *            - the event to be signaled to the agent.
	 * @return <code>true</code> if posting was successful (the container accepted new events); <code>false</code>
	 *         otherwise.
	 */
	boolean postAgentEvent(AgentEvent event);
	
	/**
	 * Returns the agent shard corresponding to the given designation.
	 * 
	 * @param designation
	 *            - the designation of the desired {@link AgentShard}.
	 * @return the {@link AgentShard} instance, if any was found; <code>null</code> otherwise.
	 */
	AgentShard getAgentShard(AgentShardDesignation designation);
}
