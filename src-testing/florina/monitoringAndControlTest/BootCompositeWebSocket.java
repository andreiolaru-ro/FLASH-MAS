package florina.monitoringAndControlTest;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootCompositeWebSocket
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

		test_args += " -package florina.monitoringAndControlTest.shards";
		test_args += " -package net.xqhs.flash.core.monitoring.shards";
		test_args += " -loader agent:composite";

		test_args += " -node node1";
		test_args += " -pylon webSocket:slave1 serverPort:8881 connectTo:ws://localhost:8881";
		test_args += " -agent composite:AgentA -shard messaging -shard ControlShard -shard MonitoringShard -shard PingBackTestComponent";

		test_args += " -node node2";
		test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8881";
		test_args += " -agent composite:AgentB -shard messaging -shard ControlShard -shard MonitoringShard";

		test_args += " -node node3";
		test_args += " -pylon webSocket:slave3 connectTo:ws://localhost:8881";
		test_args += " -agent composite:AgentC -shard messaging -shard ControlShard -shard MonitoringShard -shard PingTestComponent otherAgent:AgentA";

		FlashBoot.main(test_args.split(" "));
	}
	
}
