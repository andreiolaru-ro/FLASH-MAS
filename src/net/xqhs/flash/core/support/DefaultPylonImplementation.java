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

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
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
public class DefaultPylonImplementation extends Unit implements Pylon {
	/**
	 * The default name for instances of this implementation.
	 */
	protected static final String DEFAULT_NAME = "Default";
	
	/**
	 * Indicates whether the implementation is currently running.
	 */
	protected boolean isRunning = false;

	/**
	 * The name of this instance.
	 */
	protected String name = DEFAULT_NAME;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		this.setUnitName(getName());
		return true;
	}
	
	@Override
	public String getName() {
		return (name == null ? DEFAULT_NAME : name) + " " + CategoryName.PYLON.s();
	}
	
	@Override
	public boolean start() {
		// does nothing, only changes state.
		isRunning = true;
		lf("[] started", name);
		return true;
	}
	
	@Override
	public boolean stop() {
		// does nothing, only changes state.
		isRunning = false;
		lf("[] stopped", name);
		return true;
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		return true;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		return true;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<?> context) {
		return true;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return true;
	}

	/**
	 * The loader recommends no particular implementation for any shard.
	 */
	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardDesignation) {
		return null;
	}
	
	@Override
	public Set<String> getSupportedServices() {
		return new HashSet<>();
	}

	@Override
	public <C extends Entity<Node>> EntityProxy<C> asContext() {
		return null;
	}
}
