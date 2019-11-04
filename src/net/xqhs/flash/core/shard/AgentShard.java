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
package net.xqhs.flash.core.shard;

import net.xqhs.flash.core.ConfigurableEntity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;

/**
 * A shard (also called a component) is characterized by its functionality, identified by means of its designation -- an
 * instance of {@link AgentShardDesignation}.
 * <p>
 * The purpose of shards is to encapsulate specific functionality that can be used by an agent (e.g. messaging,
 * mobility, knowledge-management, etc). Various interfaces may extend this interface to provide APIs for common shards.
 * <p>
 * An agent shard can only work as part of an agent. As such, it has access to a proxy to the agent which must be an
 * instance of {@link ShardContainer}.
 * 
 * @author Andrei Olaru
 */
public interface AgentShard extends ConfigurableEntity<Agent>
{
	/**
	 * Retrieves the designation of the shard, so that one can know what services the shard offers. The designation
	 * should be related to the interface that the shard implements.
	 * 
	 * @return the designation of the shard (as an instance of {@link AgentShardDesignation}).
	 */
	AgentShardDesignation getShardDesignation();
	
	/**
	 * This method should be called by the agent containing the shard in order to signal to the shard that an agent
	 * event has been generated inside the agent. E.g. that the agent has been started or stopped.
	 * 
	 * @param event
	 *                  - the {@link AgentEvent} that needs to be signaled to the shard.
	 */
	void signalAgentEvent(AgentEvent event);
}
