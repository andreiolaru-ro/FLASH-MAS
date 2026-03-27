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
public class BootMovingWS {
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - If there is an argument, the argument is the index of the machine in the scenario.
	 */
	public static void main(String[] args_) {
		String args = "";
		boolean dist = false;
		
		String script = "MovingWS";
		// script += "I"; // activate the "immediate" version here
		String[] names = { "A", "B", "C", "D" };
		String server = dist ? "172.19.3.92" : "localhost";
		String serverPort = "8885";
		
		args += " -load_order monitor;pylon;agent";
		args += " -package webSocket testing src-tests.test.wsRegionsDeployment.Script test.simplePingPong -loader agent:mobileComposite ";
		
		int index = -1;
		if(args_.length > 0)
			index = Integer.parseInt(args_[0]);
		
		for(int i = index < 0 ? 0 : index; i < (index < 0 ? 4 : index + 1); i++) {
			args += " -node node" + i + " keep:10 -monitor time: -pylon webSocket:Pylon" + i;
			args += (i == 0 ? " serverPort:" : (" connectTo:ws://" + server + ":")) + serverPort;
			
			args += " -agent :" + names[i];
			args += " -shard EchoTesting";
			args += " -shard messaging -shard ScriptTesting from:" + script;
			if(i == 0)
				args += " -shard PingBackTest";
		}
		
		// MasterLog.setDefaultLogLevel(Level.OFF);
		MasterLog.enablePerformanceModeTools(1000);
		MasterLog.activateGlobalPerformanceMode();
		System.out.println(args);
		System.out.println("."); // to activate console output.
		
		FlashBoot.main(args.split(" "));
	}
	
}
