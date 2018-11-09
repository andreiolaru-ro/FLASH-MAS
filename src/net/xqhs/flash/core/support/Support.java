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

import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentFeatureType;
import net.xqhs.flash.core.node.Node;

/**
 * This interface should be implemented by any persistent entity that exists on a {@link Node} and offers to agents
 * services such as communication, mobility, directory, etc.
 * 
 * @author Andrei Olaru
 */
public interface Support extends Entity<Node>
{
	/**
	 * @return the names of services that the instance supports. FIXME: services should be better described or be part
	 *         of some class.
	 */
	public Set<String> getSupportedServices();
	
	/**
	 * Retrieves the name of the class for an agent feature implementation that is recommended by this support
	 * infrastructure, for the specified feature type, if any. If no such recommendation exists, <code>null</code> will
	 * be returned.
	 * <p>
	 * This is especially appropriate for features that depend strongly on the platform, such as messaging and mobility.
	 * Using this method, agents can be easily implemented by adding the recommended components of the platform.
	 * 
	 * @param featureType
	 *            - the type/name of the feature to be recommended.
	 * @return the name of the class containing the recommended implementation, or <code>null</code> if no
	 *         recommendation is made.
	 */
	public String getRecommendedFeatureImplementation(StandardAgentFeature featureType);
	
	/**
	 * Enrolls the given agent in the support infrastructure, making the supported services available to the agent.
	 * <p>
	 * It is expected that the implementation of this method will call {@link Entity#addContext(Entity)} to inform the
	 * agent of its inclusion in this entity.
	 * 
	 * @param agent
	 *            - the agent to be registered.
	 * @return <code>true</code> if the registration has been successful, <code>false</code> otherwise.
	 */
	public boolean registerAgent(Agent agent);
}
