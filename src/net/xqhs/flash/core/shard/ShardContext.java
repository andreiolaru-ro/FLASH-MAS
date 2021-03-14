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
