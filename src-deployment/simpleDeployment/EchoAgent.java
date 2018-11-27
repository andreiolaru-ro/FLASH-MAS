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
package simpleDeployment;

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.support.Support;
import net.xqhs.util.logging.Unit;

/**
 * Simple agent for testing.
 * 
 * @author andreiolaru
 */
@SuppressWarnings("javadoc")
public class EchoAgent extends Unit implements Agent
{
	boolean			isRunning	= false;
	Set<Support>	supports	= new HashSet<>();
	
	public EchoAgent()
	{
		setUnitName("EchoAgent");
	}
	
	@Override
	public boolean start()
	{
		li("Agent starting");
		isRunning = true;
		return true;
	}
	
	@Override
	public boolean stop()
	{
		li("Agent stopping");
		isRunning = false;
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		return isRunning;
	}
	
	@Override
	public String getName()
	{
		return "EchoAgent";
	}
	
	@Override
	public boolean addContext(Support context)
	{
		supports.add(context);
		li("Support [] added; current contexts:", context, supports);
		return true;
	}
	
	@Override
	public boolean removeContext(Support context)
	{
		if(supports.contains(context))
		{
			supports.add(context);
			li("Support [] removed; current contexts:", context, supports);
			return true;
		}
		lw("Context [] not present.", context);
		return false;
	}
	
}
