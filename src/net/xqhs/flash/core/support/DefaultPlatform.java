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

import net.xqhs.flash.core.agent.AgentComponent.AgentComponentName;
import net.xqhs.flash.core.deployment.AgentManager;
import net.xqhs.flash.core.deployment.BootSettingsManager;
import net.xqhs.flash.core.deployment.PlatformLoader;
import net.xqhs.flash.core.deployment.PlatformLoader.PlatformLink;
import net.xqhs.flash.core.deployment.PlatformLoader.StandardPlatformType;
import net.xqhs.util.XML.XMLTree.XMLNode;

/**
 * THe default platform for running agents. It is a minimal platform, offering no facilities.
 * <p>
 * Loading agents on the platform will practically have no effect on the agents.
 * 
 * @author Andrei Olaru
 */
public class DefaultPlatform implements PlatformLoader
{
	
	@Override
	public String getName()
	{
		return StandardPlatformType.DEFAULT.toString();
	}
	
	@Override
	public PlatformLoader setConfig(XMLNode configuration, BootSettingsManager settings)
	{
		// do nothing.
		return this;
	}
	
	@Override
	public boolean start()
	{
		// does nothing.
		return true;
	}
	
	@Override
	public boolean stop()
	{
		// does nothing.
		return true;
	}
	
	@Override
	public boolean addContainer(String containerName)
	{
		// does nothing.
		return true;
	}
	
	/**
	 * The method does nothing. The agents are ready to start anyway, as they need no platform support.
	 * <p>
	 * {@link AgentManager#setPlatformLink(PlatformLink)} is not called, as no support will be offered by the platform.
	 */
	@Override
	public boolean loadAgent(String containerName, AgentManager agentManager)
	{
		return true;
	}
	
	/**
	 * The loader recommends no particular implementation for any component.
	 */
	@Override
	public String getRecommendedComponentClass(AgentComponentName componentName)
	{
		return null;
	}
}
