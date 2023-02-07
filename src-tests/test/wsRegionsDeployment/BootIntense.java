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
package test.wsRegionsDeployment;

import net.xqhs.flash.FlashBoot;
import net.xqhs.util.logging.MasterLog;

/**
 * Deployment testing.
 */
public class BootIntense {
	/**
	 * Performs test.
	 * 
	 * @param args_
	 *            - If there is an argument, the argument is the index of the machine in the scenario.
	 */
	public static void main(String[] args_) {
		String args = "";
		
		String script = "Intense";
		// script += "Isolated"; // leave this here for the isolated variant
		String[] names = { "one", "two", "three", "four", "five", "six", "seven", "eight" };
		
		// do not auto-format these lines
		/* // the distributed variant.
		String[] server = { "172.19.3.92", "172.19.3.50" };
		for(int s = 0; s < 2; s++)
			server[s] = server[s] + ":8885";
		script += "Dist";
		/*/ // the single-machine variant.
		String[] server = { "localhost:8885", "localhost:8886" };
		//*/
		
		args += " -load_order monitor;pylon;agent";
		args += " -package wsRegions testing src-tests.test.wsRegionsDeployment.Script test.simplePingPong -loader agent:mobileComposite ";
		
		int index = -1;
		if(args_.length > 0)
			index = Integer.parseInt(args_[0]);
		
		for(int i = index < 0 ? 0 : index; i < (index < 0 ? 4 : index + 1); i++) {
			String srv = i % 2 == 0 ? server[i / 2] : server[(i - 1) / 2];
			args += " -node node" + i + "-" + srv + " -monitor time: -pylon WSRegions:Pylon" + i;
			args += (i % 2 == 0 ? " isServer:" : " connectTo:") + srv;
			if(i % 2 == 0)
				args += " servers:" + server[1 - i / 2];
			int index1 = i % 2 == 0 ? 0 : 4;
			for(int j = index1; j < index1 + 4; j++) {
				args += " -agent :" + names[j] + "-" + srv;
				if(j % 2 == 0)
					args += " -shard messaging -shard ScriptTesting from:" + script; // -shard EchoTesting
				else
					args += " classpath:AgentPingPong";
			}
		}
		
		// MasterLog.setDefaultLogLevel(Level.OFF);
		MasterLog.enablePerformanceModeTools(500);
		MasterLog.activateGlobalPerformanceMode();
		System.out.println("."); // to activate console output.
		
		FlashBoot.main(args.split(" "));
	}
	
}
