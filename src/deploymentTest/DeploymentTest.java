package deploymentTest;

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
		
		// test_args = "src-deployment/examples/composite/basicScenario.xml";
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
		
		test_args += " -package deploymentTest -loader agent:composite";
		test_args += " -node main";
		// test_args += " -support some";
		test_args += " -agent composite:AgentA -shard messaging -shard PingTestComponent -shard MonitoringTestShard classpath:deploymentTest.MonitoringTestShard";
		test_args += " -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";
		
		// test_args = "src-deployment/ChatAgents/deployment-chatAgents.xml";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
