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
package with_errors;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootCompositeDeployment {
	/**
	 * Scenarios with errors
	 */
	enum Scenario {
		/**
		 * 
		 */
		ALL_GOOD,
		/**
		 * 
		 */
		MISSING_NODE2,
		/**
		* 
		*/
		PYLON_MISCONFIG,
		/**
		* 
		*/
		AG1_EXISTS,
		/**
		* 
		*/
		AG2_EXISTS
	}
	
	/**
	 * Designation for shards.
	 */
	public static final String	FUNCTIONALITY	= "TESTING";
	/**
	 * Different designation for shards.
	 */
	public static final String	MONITORING		= "MONITORING";
	
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_) {
		Scenario scenario = Scenario.ALL_GOOD;
		// scenario = Scenario.MISSING_NODE2;
		// scenario = Scenario.PYLON_MISCONFIG;
		// scenario = Scenario.AG1_EXISTS;
		// scenario = Scenario.AG2_EXISTS;
		
		String pkg = " -package testing -loader agent:composite";
		
		String pylon1 = " -pylon webSocket:pylon1 serverPort:8886";
		String pylon2 = " -pylon webSocket:pylon2 connectTo:ws://localhost:8886";
		String pylon2Misconfig = pylon2.substring(0, pylon2.length() - 1) + "7";
		
		String agent1 = " -agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentB -shard EchoTesting";
		String agent1wExit = agent1 + " exit:10";
		
		String agent2 = " -agent composite:AgentB -shard messaging -shard PingBackTest -shard EchoTesting";
		String agent2wExit = agent1 + " exit:10";
		
		String args = "";
		switch(scenario) {
		case MISSING_NODE2:
			args = pkg + " -node node1" + pylon1 + agent1;
			break;
		case PYLON_MISCONFIG:
			args = pkg + " -node node1" + pylon1 + agent1 + " -node node2" + pylon2Misconfig + agent2;
			break;
		case AG1_EXISTS:
			args = pkg + " -node node1" + pylon1 + agent1wExit + " -node node2" + pylon2 + agent2;
			break;
		case AG2_EXISTS:
			args = pkg + " -node node1" + pylon1 + agent1 + " -node node2" + pylon2 + agent2wExit;
			break;
		default:
			args = pkg + " -node node1" + pylon1 + agent1 + " -node node2" + pylon2 + agent2;
			break;
		}
		FlashBoot.main(args.split(" "));
	}
	
}
