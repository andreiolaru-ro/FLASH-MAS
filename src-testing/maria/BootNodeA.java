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
package maria;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;

/**
 * Deployment testing.
 */
public class BootNodeA {
	public static String	MAIN_IP		= "192.168.100.3";
	public static int		MAIN_PORT	= 8886;
	
	/**
	 * Runs Node A.
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_) {
		String test_args = "";
		test_args += " -package maria example.compositePingPong -loader agent:composite";
		
		test_args += " -node nodeA";
		test_args += " -pylon webSocket:wsA serverPort:" + Integer.valueOf(MAIN_PORT);
		/*
		test_args += " -agent agentA1 classpath:maria.MobileCompositeAgent -shard messaging -shard PingTestComponent otherAgent:agentB1 -shard MonitoringTestShard";
		 /*/
		test_args += " -agent agentA1 classpath:maria.MobileCompositeAgent -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";
		// */
		test_args += " -agent agentA2 -shard messaging -shard MonitoringTestShard";
		
		List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(test_args.split(" ")));
		for(Node node : nodes) {
			node.start();
			System.out.println(node.getName() + " has the following entities: " + node.entityOrder);
		}
		
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		MobileCompositeAgent agentA1 = (MobileCompositeAgent) Boot.getAgent(nodes, "agentA1");
		
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		agentA1.moveTo("nodeB");
		
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		for(Node node : nodes) {
			System.out.println(node.getName() + " has the following entities: " + node.entityOrder);
			// node.stop();
		}
	}
}
