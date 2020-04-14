package test.webSocketsDeployment;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootCompositeDeployment
{
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
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";
		
		test_args += " -package test.compositePingPong -loader agent:composite";
		
		test_args += " -node node1";
		test_args += " -pylon webSocket:slave1 serverPort:8886 connectTo:ws://localhost:8886";
		test_args += " -agent composite:AgentA -shard messaging -shard PingTestComponent otherAgent:AgentB -shard MonitoringTest";
		
		test_args += " -node node2";
		test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8886";
		test_args += " -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
