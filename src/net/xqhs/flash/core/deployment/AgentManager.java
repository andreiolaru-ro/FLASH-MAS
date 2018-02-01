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
package net.xqhs.flash.core.deployment;

import tatami.simulation.PlatformLoader.PlatformLink;

/**
 * An instance implementing this interface is a wrapper to an actual agent instance (that may be not yet created). It
 * offers methods for managing the lifecycle of an agent, for instance starting and stopping.
 * 
 * @author Andrei Olaru
 */
public interface AgentManager
{
	/**
	 * Starts the agent. If this goes well, from this moment on the agent should be executing normally.
	 * <p>
	 * The method must guarantee that once it has been started, it can immediately begin to receive events, even if
	 * those events will not be processed immediately.
	 * 
	 * @return <code>true</code> if the agent was started without error. <code>false</code> otherwise.
	 */
	public boolean start();
	
	/**
	 * Stops the agent. After this method succeeds, the agent should not be executing any more.
	 * 
	 * @return <code>true</code> if the agent was stopped without error. <code>false</code> otherwise.
	 */
	public boolean stop();
	
	/**
	 * Queries the agent to check if the agent has completed its startup and is fully functional. The agent is running
	 * after it has fully started and until it is {@link #stop}ed.
	 * 
	 * @return <code>true</code> if the agent is currently running.
	 */
	public boolean isRunning();
	
	/**
	 * Creates a link from the agent to the platform, which will facilitate the invocation of specific platform
	 * functionality. The passed instance may be the platform itself, or some agent-specific instance, depending on the
	 * platform.
	 * <p>
	 * This method can usually by called only when the agent is not running.
	 * 
	 * @param link
	 *            - the link to the platform.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean setPlatformLink(PlatformLink link);
	
	/**
	 * Retrieves the name of the agent managed by this instance.
	 * 
	 * @return the name of the agent.
	 */
	public String getAgentName();
}
