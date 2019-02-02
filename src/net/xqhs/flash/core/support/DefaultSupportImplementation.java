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

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;

/**
 * The default platform for running agents. It is a minimal platform, offering no services.
 * <p>
 * The class extends {@link Unit} so as to make logging easy for extending implementations.
 * <p>
 * Loading agents on the platform will practically have no effect on the agents.
 * 
 * @author Andrei Olaru
 */
public class DefaultSupportImplementation extends Unit implements Support
{
	/**
	 * The default name for instances of this implementation.
	 */
	protected static final String	DEFAULT_NAME	= "Default Support ";
	
	/**
	 * Indicates whether the implementation is currently running.
	 */
	protected boolean	isRunning		= false;
	
	/**
	 * The name of this instance.
	 */
	protected String				name			= DEFAULT_NAME;
	
	@Override
	public boolean configure(MultiTreeMap configuration)
	{
		if(configuration.isSimple("name"))
			name = configuration.get("name");
		return true;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean start()
	{
		// does nothing, only changes state.
		isRunning = true;
		lf("[] started", name);
		return true;
	}
	
	@Override
	public boolean stop()
	{
		// does nothing, only changes state.
		isRunning = false;
		lf("[] stopped", name);
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		return isRunning;
	}
	
	@Override
	public boolean addContext(Node context)
	{
		// context has no effect on the default implementation
		return true;
	}
	
	@Override
	public boolean addGeneralContext(Entity<?> context)
	{
		// context has no effect on the default implementation
		return true;
	}
	
	@Override
	public boolean removeContext(Node context)
	{
		throw new UnsupportedOperationException("Cannot remove context from a node");
	}
	
	/**
	 * The loader recommends no particular implementation for any component.
	 */
	@Override
	public String getRecommendedFeatureImplementation(StandardAgentShard componentName)
	{
		return null;
	}
	
	/**
	 * The default implementation informs the agent that it has been added to the context of this support
	 * infrastructure.
	 */
	@Override
	public boolean registerAgent(Agent agent)
	{
		agent.addContext(this);
		lf("[] registered agent", name, agent);
		return true;
	}
	
	@Override
	public Set<String> getSupportedServices()
	{
		return new HashSet<>();
	}
}
