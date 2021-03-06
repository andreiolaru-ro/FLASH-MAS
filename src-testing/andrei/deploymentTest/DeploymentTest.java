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
package andrei.deploymentTest;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class DeploymentTest
{
	/**
	 * Performs test.
	 * 
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";
		
		// empties
		
		// test_args = "src-deployment/examples/echoAgent/simpleDeployment.xml";
		
		// configuration testing
		
		test_args += "old-deployment/composite/basicScenario.xml";
		// test_args = "-support local:auxilliary host:here -agent bane something:something -shard a";
		// test_args = "src-deployment/ComplexDeployment/complexDeployment.xml -agent AgentA some:property -shard
		// mobility where:anywhere host:here -agent bane something:something -othercomponent a -support custom par:val
		// -node node2 new:val";
		// test_args = "src-deployment/ComplexDeployment/complexDeployment-autonode.xml -agent AgentA some:property
		// -shard mobility where:anywhere host:here -agent bane something:something -othercomponent a -support custom
		// par:val";
		// test_args = "-support local -support local arg:val -support last host:here -agent bane something:something
		// -shard a -shard b par:val -shard c -agent bruce -shard a";
		
		// simple deployments
		
		// test_args += " -node main";
		test_args += " -package examples.compositePingPong -loader agent:composite";
		// test_args += " -support some";
		test_args += " -agent composite:AgentA -shard messaging -shard PingTestComponent otherAgent:AgentB -shard MonitoringTest";
		test_args += " -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";
		
		// test_args = "-package deploymentTest -agent AgentA classpath:TestAgent";
		
		// test_args = "src-deployment/ChatAgents/deployment-chatAgents.xml";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
