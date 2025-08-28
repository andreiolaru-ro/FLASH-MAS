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
package andrei.abms;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class Boot
{
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		String a = "";
		
		a += " -load_order map;executor;AgentGroup";
		a += " -package andrei.abms";
		a += " -loader AgentGroup:AgentGroup";
		a += " -node main";
		a += " -map Grid vicinity:8neighbors classpath:gridworld.GridMap";
		a += " -executor StepWise: steps:10";
		a += " -AgentGroup g d:5 classpath:AgentGroup -agent CA";
		
		FlashBoot.main(a.split(" "));
	}
	
}
