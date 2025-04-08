package mihai;

import net.xqhs.flash.FlashBoot;

public class BootMultiPlatformWebSocket {
	/**
	 * Performs test
	 * 
	 * @param args_ - not used.
	 */

	public static void main(String[] args_) {
		String args = "";
		args += " -package wsRegions testing mihai -loader agent:composite -load_order pylon;agent;bridge";

		args += " -node node1 keep:50";
		args += " -pylon webSocket:pylonA1 isServer:localhost:8885";
		args += " -pylon webSocket:pylonB1 isServer:localhost:8886";
		args += " -bridge interoperability:bridge1 in-context-of:webSocket:pylonA1";

		args += " -node node2 keep:50";
		args += " -pylon webSocket:pylonA2 connectTo:ws://localhost:8885";
		args += " -pylon webSocket:pylonC1 isServer:localhost:8887";
		args += " -bridge interoperability:bridge2 in-context-of:webSocket:pylonA2";

		args += " -node nodeA keep:1";
		args += " -pylon webSocket:pylonA3 connectTo:ws://localhost:8885";
		args += " -agent agentA -shard messaging -shard EchoTesting exit:30";

		args += " -node nodeB keep:1";
		args += " -pylon webSocket:pylonB2 connectTo:ws://localhost:8886";
		args += " -agent agentB -shard messaging -shard EchoTesting -shard PingTest otherAgent:ws://localhost:8887/agentC";

		args += " -node nodeC keep:1";
		args += " -pylon webSocket:pylonC2 connectTo:ws://localhost:8887";
		args += " -agent agentC -shard messaging -shard EchoTesting exit:50";

		FlashBoot.main(args.split(" "));
	}
}
