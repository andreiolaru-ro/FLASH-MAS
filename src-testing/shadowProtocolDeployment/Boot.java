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

/**
 * Deployment testing.
 */
public class Boot
{
	/**
	 * Designation for shards.
	 */
	public static final String	FUNCTIONALITY	= "TESTING";
	/**
	 * Different designation for shards.
	 */
	public static final String	MONITORING		= "OTHER-MONITORING";
	
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		// TestClass test = new
		// TestClass("src-testing/shadowProtocolDeployment/RandomTestCases/topology1_2_servers_2_pylons_2_agents.json");

		String args = "";

		args += " -package wsRegions testing src-testing.shadowProtocolDeployment.Scripts -loader agent:mobileComposite ";

		args += " -node node1";
		args += " -pylon WSRegions:Pylon1 isServer:localhost:8885";
		args += " -agent :one-localhost:8885 -shard messaging -shard EchoTesting -shard ScriptTesting from:Simple";
		args += " -agent :two-localhost:8885 -shard messaging -shard EchoTesting -shard ScriptTesting from:Simple";
		args += " -node node2";
		args += " -pylon WSRegions:Pylon2 connectTo:localhost:8885";

		// args += " -node node2";
		// args += " -pylon Pylon-Two classpath:net.xqhs.flash.shadowProtocol.ShadowPylon connectTo:ws://localhost:8886
		// serverPort:8886 servers:" + String.join("|", test.regionServersList) + " pylon_name:Pylon-Two";
		// args += " -agent two-localhost:8886 classpath:shadowProtocolDeployment.CompositeAgentTest -shard messaging
		// ShadowAgentShard connectTo:ws://localhost:8886 agent_name:two-localhost:8886 -shard SendMessageShard
		// agent_name:two-localhost:8886";

		FlashBoot.main(args.split(" "));
	}
	
}
