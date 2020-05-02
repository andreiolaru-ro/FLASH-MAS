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

		test_args += " -package florina.monitoringAndControlTest -loader agent:composite";

		test_args += " -node node1";
		test_args += " -pylon webSocket:slave1 serverPort:8885 connectTo:ws://localhost:8885";
		test_args += " -agent composite:AgentA -shard messaging -shard ControlShardTest";
		test_args += " -agent composite:AgentB -shard messaging -shard ControlShardTest";

		test_args += " -node node2";
		test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8885";
		test_args += " -agent composite:AgentC -shard messaging -shard ControlShardTest";


		FlashBoot.main(test_args.split(" "));
	}
	
}
