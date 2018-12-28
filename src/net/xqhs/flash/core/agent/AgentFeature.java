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
package net.xqhs.flash.core.agent;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.composite.AgentFeatureDesignation;

/**
 * A feature (also called a component) is characterized by its functionality, identified by means of its designation --
 * an instance of {@link AgentFeatureDesignation}.
 * 
 * @author andreiolaru
 */
public interface AgentFeature extends Entity<Agent>
{
	/**
	 * @return the designation of the feature (instance of {@link AgentFeatureDesignation}).
	 */
	AgentFeatureDesignation getFeatureDesignation();
}
