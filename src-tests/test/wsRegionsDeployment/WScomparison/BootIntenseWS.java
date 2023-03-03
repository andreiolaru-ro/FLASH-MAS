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
package test.wsRegionsDeployment.WScomparison;

import net.xqhs.flash.FlashBoot;
import net.xqhs.util.logging.MasterLog;

/**
 * Deployment testing.
 */
public class BootIntenseWS {
	/**
	 * Performs test.
	 * 
	 * @param args_
	 *            - If there is an argument, the argument is the index of the machine in the scenario.
	 */
	public static void main(String[] args_) {
		String args = "";
		boolean dist = false;
		
		String script = "IntenseWS";
		String[] names = new String[16];
		for(int i = 1; i <= 16; i++)
			names[i - 1] = Integer.valueOf(i).toString();
		String server = dist ? "172.19.3.92" : "localhost";
		String serverPort = "8885";
		
		args += " -load_order monitor;pylon;agent";
		args += " -package webSocket testing src-tests.test.wsRegionsDeployment.Script test.simplePingPong -loader agent:mobileComposite ";
		
		int index = -1;
		if(args_.length > 0)
			index = Integer.parseInt(args_[0]);
		
		for(int i = index < 0 ? 0 : index; i < (index < 0 ? 4 : index + 1); i++) {
			args += " -node node" + i + " -monitor time: -pylon webSocket:Pylon" + i;
			args += (i == 0 ? " serverPort:" : (" connectTo:ws://" + server + ":")) + serverPort;
			
			int index1 = i * 4;
			for(int j = index1; j < index1 + 4; j++) {
				args += " -agent :A" + names[j];
				if(j % 2 == 0)
					args += " -shard messaging -shard ScriptTesting from:" + script; // -shard EchoTesting
				else
					args += " classpath:AgentPingPong";
			}
		}
		
		// MasterLog.setDefaultLogLevel(Level.OFF);
		MasterLog.enablePerformanceModeTools(500);
		MasterLog.activateGlobalPerformanceMode();
		System.out.println(args);
		System.out.println("."); // to activate console output.
		
		FlashBoot.main(args.split(" "));
	}
	
}
