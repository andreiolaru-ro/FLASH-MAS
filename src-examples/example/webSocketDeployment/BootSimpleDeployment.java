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
package example.webSocketDeployment;

import net.xqhs.flash.FlashBoot;

/**
 * Tests WebSocket support works in a deployment of non-composite agents.
 */
public class BootSimpleDeployment
{
	/**
	 * Performs test.a
	 * 
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";
		
		test_args += " -package example.simplePingPong";
		
		test_args += " -node node1";
		test_args += " -pylon webSocket:slave1 serverPort:8885 connectTo:ws://localhost:8885";
		test_args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
		
		test_args += " -node node2";
		test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8885";
		test_args += " -agent AgentB classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
