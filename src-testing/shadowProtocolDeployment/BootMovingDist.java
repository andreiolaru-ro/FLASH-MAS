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
public class BootMovingDist {
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_) {
		String args = "";
		
		String[] names = { "A", "B", "C", "D" };
		String[] server = { "172.19.3.92", "172.19.3.50" };
		for(int s = 0; s < 2; s++)
			server[s] = server[s] + ":8885";
		
		args += " -load_order monitor;pylon;agent";
		args += " -package wsRegions testing src-testing.shadowProtocolDeployment.Scripts test.simplePingPong -loader agent:mobileComposite ";
		
		int i = 0;// Integer.parseInt(args_[0]);
		
		// for(int i = 0; i < 4; i++) {
		String srv = i % 2 == 0 ? server[i / 2] : server[(i - 1) / 2];
		args += " -node node" + i + "-" + srv + " -monitor time: -pylon WSRegions:Pylon" + i;
		args += (i % 2 == 0 ? " isServer:" : " connectTo:") + srv;
		if(i % 2 == 0)
			args += " servers:" + server[1 - i / 2];
		args += " -agent :" + names[i] + "-" + srv;
		args += " -shard EchoTesting";
		args += " -shard messaging -shard ScriptTesting from:MovingDist";
		if(i == 0)
			args += " -shard PingBackTest";
		// }
		
		// MasterLog.setDefaultLogLevel(Level.OFF);
		MasterLog.enablePerformanceModeTools(1000);
		MasterLog.activateGlobalPerformanceMode();
		System.out.println("."); // to activate console output.
		
		FlashBoot.main(args.split(" "));
	}
	
}
