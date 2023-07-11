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
package test.compositeMobility;

import java.util.Arrays;

import net.xqhs.flash.core.node.NodeLoader;

/**
 * Deployment testing.
 */
public class BootNodeB
{
	/**
	 * Runs Node B.
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		String a = "";
		a += Boot.prelude;
		
		a += " -node nodeB";
		a += " -pylon webSocket:wsB connectTo:ws://" + BootNodeA.MAIN_IP + ":" + Integer.valueOf(BootNodeA.MAIN_PORT);
		a += Boot.nodeB_agents;
		
		new NodeLoader().loadDeployment(Arrays.asList(a.split(" "))).forEach(node -> node.start());
	}
}
