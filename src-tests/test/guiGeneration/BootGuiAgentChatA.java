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
package test.guiGeneration;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootGuiAgentChatA
{
	/**
	 * The IP address of the main node.
	 */
	public static String	MAIN_IP		= "localhost";
	/**
	 * The port on the main node.
	 */
	public static int		MAIN_PORT	= 8886;
	
	/**
	 * Performs test.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";

		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				BootGuiAgentChatB.main(null);
			}
		}, 3000);
		
		test_args += " -loader agent:composite";
		test_args += " -package test.guiGeneration";

		test_args += " -node main central:web";
		test_args += " -pylon webSocket:wsA serverPort:" + Integer.valueOf(MAIN_PORT);
		test_args += " -agent composite:AgentA -shard messaging -shard remoteOperation wait:2000 -shard swingGui from:basic-chat.yml -shard BasicChat otherAgent:AgentB playerNumber:1";
		
		FlashBoot.main(test_args.split(" "));
		
	}
	
}
