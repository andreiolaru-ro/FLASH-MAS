package test.compositePingPong;

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
	public static final String	MONITORING		= "MONITORING";
	
	/**
	 * Performs test.
	 * 
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";
		
		test_args += " -package test.compositePingPong -loader agent:composite";
		test_args += " -agent composite:AgentA -shard messaging -shard PingTestComponent otherAgent:AgentB -shard MonitoringTest";
		test_args += " -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
