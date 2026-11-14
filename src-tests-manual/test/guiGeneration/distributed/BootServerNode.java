package test.guiGeneration.distributed;

import net.xqhs.flash.FlashBoot;

public class BootServerNode {

	public static final int PYLON_PORT = 8886;

	public static void main(String[] args) {
		String test_args = "";

		test_args += " -loader agent:composite";
		test_args += " -package test.guiGeneration";

		test_args += " -node nodeA";
		test_args += " -pylon webSocket:serverPylon serverPort:" + PYLON_PORT;

		test_args += " -agent composite:AgentA";
		test_args += " -shard messaging";

		test_args += " -shard swingGui from:basic-chat.yml";
		test_args += " -shard remoteOperation wait:2000";
		test_args += " -shard BasicChat otherAgent:AgentB";

		test_args += " -agent :Monitoring&Control_Entity";
		test_args += " classpath:net.xqhs.flash.remoteOperation.CentralMonitoringAndControlEntity";
		test_args += " central:web";

		FlashBoot.main(test_args.split(" "));
	}
}