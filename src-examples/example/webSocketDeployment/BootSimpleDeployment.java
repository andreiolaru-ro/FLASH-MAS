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
	 * Performs test.
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		String args = "";
		
		args += " -package example.simplePingPong";
		
		args += " -node node1";
		args += " -pylon webSocket:slave1 serverPort:8885";
		args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
		
		args += " -node node2";
		args += " -pylon webSocket:slave2 connectTo:ws://localhost:8885";
		args += " -agent AgentB classpath:AgentPingPong";
		
		FlashBoot.main(args.split(" "));
	}
	
}
