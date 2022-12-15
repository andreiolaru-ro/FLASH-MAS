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
package shadowProtocolDeployment;

import net.xqhs.flash.FlashBoot;
import net.xqhs.util.logging.MasterLog;

/**
 * Deployment testing.
 */
public class BootIntense {
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_) {
		String args = "";
		
		String[] names = { "one", "two", "three", "four", "five", "six", "seven", "eight" };
		String[] server = { "localhost:8885", "localhost:8886" };
		
		args += " -load_order monitor;pylon;agent";
		args += " -package wsRegions testing src-testing.shadowProtocolDeployment.Scripts test.simplePingPong -loader agent:mobileComposite ";
		
		for(int i = 0; i < 4; i++) {
			String srv = i % 2 == 0 ? server[i / 2] : server[(i - 1) / 2];
			args += " -node node" + i + "-" + srv + " -monitor time: -pylon WSRegions:Pylon" + i;
			args += (i % 2 == 0 ? " isServer:" : " connectTo:") + srv;
			if(i % 2 == 0)
				args += " servers:" + server[1 - i / 2];
			int index = i % 2 == 0 ? 0 : 4;
			for(int j = index; j < index + 4; j++) {
				args += " -agent :" + names[j] + "-" + srv;
				if(j % 2 == 0)
					args += " -shard messaging -shard ScriptTesting from:IntenseIsolated"; // -shard EchoTesting
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
