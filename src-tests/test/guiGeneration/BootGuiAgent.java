package test.guiGeneration;

import net.xqhs.flash.FlashBoot;

public class BootGuiAgent
{
	public static void main(String[] args)
	{
		String test_args = "";

		test_args += " -loader agent:composite";
		test_args += " -package test.guiGeneration";

		test_args += " -node main central:web";

		test_args += " -pylon webSocket:serverPylon serverPort:8886";

//		test_args += " -agent :Monitoring&Control_Entity";
//		test_args += " classpath:net.xqhs.flash.remoteOperation.CentralMonitoringAndControlEntity";
//		test_args += " central:web";
//		test_args += " -shard messaging";

		test_args += " -agent composite:AgentA -shard messaging -shard remoteOperation -shard swingGui from:basic-chat.yml -shard BasicChat otherAgent:AgentB playerNumber:1";
		test_args += " -agent composite:AgentB -shard messaging -shard remoteOperation -shard swingGui from:basic-chat.yml -shard BasicChat otherAgent:AgentA playerNumber:2";

		FlashBoot.main(test_args.split(" "));
	}
}