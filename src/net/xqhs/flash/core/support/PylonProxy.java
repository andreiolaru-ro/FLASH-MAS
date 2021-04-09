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

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.shard.AgentShardDesignation;

/**
 * {@link EntityProxy} for a {@link Pylon}, specifying the only one required method, for retrieving the recommended
 * implementation for shards connecting to the pylon.
 * 
 * @author Andrei Olaru
 */
public interface PylonProxy extends EntityProxy<Pylon>
{
	/**
	 * Retrieves the name of the class for an agent shard implementation that is recommended by this support
	 * infrastructure, for the specified shard type, if any. If no such recommendation exists, <code>null</code> will be
	 * returned.
	 * <p>
	 * 
	 * @see Pylon
	 * 
	 * @param shardType
	 *                      - the type/name of the shard to be recommended.
	 * @return the name of the class containing the recommended implementation, or <code>null</code> if no
	 *         recommendation is made.
	 */
	public String getRecommendedShardImplementation(AgentShardDesignation shardType);
}
