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

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.Node.NodeProxy;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.util.logging.Unit;

/**
 * Pylon for the default support infrastructure for agents. It is a minimal infrastructure, offering no services.
 * <p>
 * The class extends {@link Unit} so as to make logging easy for extending implementations.
 * <p>
 * Adding agents in the context of the pylon will practically have no effect on the agents.
 * 
 * @author Andrei Olaru
 */
public class DefaultPylonImplementation extends EntityCore<Node> implements Pylon {
	/**
	 * The default name for instances of this implementation.
	 */
	protected static final String DEFAULT_NAME = "Default";
	
	/**
	 * The name of the node in the context of which this pylon is placed.
	 */
	protected String nodeName;
	
	@Override
	public String getName() {
		return (name == null ? DEFAULT_NAME : name) + " " + CategoryName.PYLON.s();
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		super.addContext(context);
		nodeName = context.getEntityName();
		lf("Added node context:", nodeName);
		return true;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		if(nodeName == null)
			return ler(false, "No context was present, nothing to remove.");
		lf("Context removed:", nodeName);
		nodeName = null;
		return true;
	}
	
	@Override
	public boolean isMainContext(Object context) {
		return context instanceof NodeProxy;
	}
	
	/**
	 * The pylon recommends no particular implementation for any shard.
	 */
	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardDesignation) {
		return null;
	}
	
	@Override
	public Set<String> getSupportedServices() {
		return new HashSet<>();
	}
}
