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
package andrei.deploymentTest;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class NodesTest
{
	/**
	 * Performs test.
	 * 
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";
		
		test_args += " -loader agent classpath:core.composite.CompositeAgentLoader";
		test_args += " -node nodeA -agent agentA1 -agent agentA2 -agent agentA3";
		test_args += " -node nodeB -agent agentB1 -agent agentB2 -agent agentB3";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
