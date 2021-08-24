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

	public static final int N = 1000;

	public static void main(String[] args)
	{
		String test_args = "";

		test_args += " -package florina.monitoringAndControlTest.shards";
		test_args += " -package net.xqhs.flash.core.monitoring.shards";
		test_args += " -loader agent:composite";

		test_args += " -node node1";
		test_args += " -pylon webSocket:slave1 serverPort:8881 connectTo:ws://localhost:8881";
		test_args += " -agent composite:AgentA -shard messaging -shard ControlShard -shard MonitoringShard -shard PingTestComponent";
		for (int i = 0; i < N; i++) {
			test_args += " otherAgent:" + i;
		}

		test_args += " -node node2";
		test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8881";
		for (int i = 0; i < N; i++) {
			test_args += " -agent composite:" + i + " -shard messaging -shard ControlShard -shard MonitoringShard -shard PingBackTestComponent";
		}

		FlashBoot.main(test_args.split(" "));
	}
	
}
