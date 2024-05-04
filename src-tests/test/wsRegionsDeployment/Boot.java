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

/**
 * Deployment testing.
 */
public class Boot
{
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		String args = "", script = "";
		
		script = " -shard ScriptTesting from:Simple";

		args += " -load_order monitor;pylon;agent";
		args += " -package net.xqhs.flash.wsRegions testing src-tests.test.wsRegionsDeployment.Script -loader agent:mobileComposite ";

		args += " -node node1-localhost:8885 -monitor time:";
		args += " -pylon WSRegions:Pylon1 isServer:localhost:8885";
		args += " -agent :one-localhost:8885 -shard messaging -shard EchoTesting" + script;
		args += " -agent :two-localhost:8885 -shard messaging -shard EchoTesting" + script;
		args += " -node node2-localhost:8885 keep:10";
		args += " -pylon WSRegions:Pylon2 connectTo:localhost:8885";

		// MasterLog.enablePerformanceModeTools(500);
		// MasterLog.activateGlobalPerformanceMode();
		System.out.println("."); // to activate console output.
		
		FlashBoot.main(args.split(" "));
	}
	
}
