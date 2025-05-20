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

		args += " -node nodeA keep:50";
		args += " -pylon webSocket:pylonA isServer:localhost:8885 serverName:WS-A";
//		args += " -agent agentA -shard messaging -shard EchoTesting exit:30";

		args += " -node nodeB keep:50";
		args += " -pylon webSocket:pylonB isServer:localhost:8886 serverName:WS-B";
		args += " -agent agentB -shard messaging -shard EchoTesting -shard PingTest otherAgent:ws://localhost:8887/agentC";

		args += " -node nodeC keep:50";
		args += " -pylon webSocket:pylonC isServer:localhost:8887 serverName:WS-C";
		args += " -agent agentC -shard messaging -shard EchoTesting exit:50";

		args += " -node node1 keep:50";
		args += " -pylon webSocket:pylon1A connectTo:ws://localhost:8885";
		args += " -pylon webSocket:pylon1B connectTo:ws://localhost:8886";
		args += " -bridge interoperability:bridge1 in-context-of:webSocket:pylon1A";

		args += " -node node2 keep:50";
		args += " -pylon webSocket:pylon2A connectTo:ws://localhost:8885";
		args += " -pylon webSocket:pylon2C connectTo:ws://localhost:8887";
		args += " -bridge interoperability:bridge2 in-context-of:webSocket:pylon2A";

		FlashBoot.main(args.split(" "));
	}
}
