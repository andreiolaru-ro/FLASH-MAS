package mihai;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 * 
 * AgentC (in a WebSocket infrastructure) sends messages to AgentA (in a WSRegions infrastructure), via the bridge in
 * nodeB. A number of 5 pings are sent.
 */
public class BootWSRegionsToWebSocket {
	/**
	 * Performs test
	 * 
	 * @param args_ - not used.
	 */

	public static void main(String[] args_) {
		String args = "";
		args += " -package wsRegions testing mihai -loader agent:composite -load_order pylon;agent;bridge";
		
		args += " -node nodeB keep:20";
		args += " -pylon webSocket:pylonWebsocket isServer:localhost:8886";
		args += " -pylon WSRegions:pylonWSRegions isServer:localhost:8885";
		args += " -bridge interoperability:bridge1 in-context-of:webSocket:pylonWebsocket";

		args += " -node nodeA keep:1";
		args += " -pylon WSRegions:pylonA connectTo:localhost:8885";
		args += " -agent :ws://localhost:8885/agentA -shard messaging -shard EchoTesting -shard PingTest otherAgent:ws://localhost:8886/agentC";

		args += " -node nodeC keep:1";
		args += " -pylon webSocket:pylonC connectTo:ws://localhost:8886";
		args += " -agent agentC -shard messaging -shard EchoTesting exit:30";

		FlashBoot.main(args.split(" "));
	}
}