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

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import test.webSocketDeployment.multipleMachines.BootCompositeNodeA;

/**
 * Deployment testing.
 */
public class BootNodeB
{
	/**
	 * Runs Node B.
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		String test_args = "";
		test_args += " -package maria example.compositePingPong -loader agent:composite";
		
        test_args += " -node nodeB";
		test_args += " -pylon webSocket:wsB connectTo:ws://" + BootCompositeNodeA.MAIN_IP + ":"
				+ Integer.valueOf(BootCompositeNodeA.MAIN_PORT);
		/*
		test_args += " -agent agentB1 -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";
/*/     
        test_args += " -agent agentB1 -shard messaging -shard PingTestComponent otherAgent:agentA1 -shard MonitoringTestShard";
//*/

        List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(test_args.split(" ")));
        for(Node node : nodes) {
            node.start();
            System.out.println(node.getName() + " has the following entities: " + node.entityOrder);
        }
	}
	
}
