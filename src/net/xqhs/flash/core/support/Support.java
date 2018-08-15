/*******************************************************************************
 * Copyright (C) 2015 Andrei Olaru, Marius-Tudor Benea, Nguyen Thi Thuy Nga, Amal El Fallah Seghrouchni, Cedric Herpson.
 * 
 * This file is part of tATAmI-PC.
 * 
 * tATAmI-PC is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * tATAmI-PC is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with tATAmI-PC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.support;

import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentFeature.AgentFeatureType;
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
	 * @return the names of services that the instance supports.
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
	public String getRecommendedFeatureImplementation(AgentFeatureType featureType);
}
