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

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentFeature.AgentFeatureType;
import net.xqhs.flash.core.node.Node;
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
	 * Indicates whether the implementation is currently running.
	 */
	protected boolean	isRunning		= false;
	
	/**
	 * The name for instances of this implementation.
	 */
	protected String	DEFAULT_NAME	= "Default Support";
	
	@Override
	public String getName()
	{
		return DEFAULT_NAME + " " + this.toString();
	}
	
	@Override
	public boolean start()
	{
		// does nothing, only changes state.
		isRunning = true;
		lf(DEFAULT_NAME + " started");
		return true;
	}
	
	@Override
	public boolean stop()
	{
		// does nothing, only changes state.
		isRunning = false;
		lf(DEFAULT_NAME + " stopped");
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
	
	/**
	 * The loader recommends no particular implementation for any component.
	 */
	@Override
	public String getRecommendedFeatureImplementation(AgentFeatureType componentName)
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
		lf(DEFAULT_NAME + " registered agent", agent);
		return true;
	}
	
	@Override
	public Set<String> getSupportedServices()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
