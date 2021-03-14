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
package laura;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootSimple {
	/**
	 * Performs test.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		String test_args = "";
		
		test_args += " -package test.simplePingPong";
		
		// ce merge acum:
		test_args += " -node nodeA";
		test_args += " -pylon local:";
		test_args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2";
		test_args += " -agent agentA2 classpath:AgentPingPong";
		test_args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2";
		test_args += " -agent agentB2 classpath:AgentPingPong";
		
		// ce vrem să meargă:
		// test_args += " -node nodeA";
		// test_args += " -support local classpath:net.xqhs.flash.local.LocalSupport";
		// test_args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2";
		// test_args += " -agent agentA2 classpath:AgentPingPong";
		// test_args += " -node nodeB";
		// test_args += " -support local classpath:net.xqhs.flash.local.LocalSupport";
		// test_args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2";
		// test_args += " -agent agentB2 classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
