package mihai;

import net.xqhs.flash.FlashBoot;

public class BootMultiPlatformWSRegions {
	/**
	 * Performs test
	 * 
	 * @param args_ - not used.
	 */

	public static void main(String[] args_) {
		String args = "";
		args += " -package wsRegions testing mihai -loader agent:composite -load_order pylon;agent;bridge";

		args += " -node nodeA keep:50";
		args += " -pylon WSRegions:pylonA isServer:localhost:8885";
//		args += " -agent agentA -shard messaging -shard EchoTesting exit:30";

		args += " -node nodeB keep:50";
		args += " -pylon WSRegions:pylonB isServer:localhost:8886";
		args += " -agent :ws://localhost:8886/agentB -shard messaging -shard EchoTesting -shard PingTest otherAgent:ws://localhost:8887/agentC";

		args += " -node nodeC keep:50";
		args += " -pylon WSRegions:pylonC isServer:localhost:8887";
		args += " -agent :ws://localhost:8887/agentC -shard messaging -shard EchoTesting exit:50";

		args += " -node node1 keep:50";
		args += " -pylon WSRegions:pylon1A connectTo:localhost:8885";
		args += " -pylon WSRegions:pylon1B connectTo:localhost:8886";
		args += " -bridge interoperability:bridge1 in-context-of:WSRegions:pylon1A";

		args += " -node node2 keep:50";
		args += " -pylon WSRegions:pylon2A connectTo:localhost:8885";
		args += " -pylon WSRegions:pylon2C connectTo:localhost:8887";
		args += " -bridge interoperability:bridge2 in-context-of:WSRegions:pylon2A";

		FlashBoot.main(args.split(" "));
	}
}
