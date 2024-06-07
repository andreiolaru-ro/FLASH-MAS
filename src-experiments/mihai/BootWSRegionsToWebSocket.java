package mihai;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootWSRegionsToWebSocket {
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */

	public static void main(String[] args_) {
		String args = "";
		args += " -package wsRegions mihai testing src-tests.test.wsRegionsDeployment.Script -loader agent:composite -load_order pylon;agent;bridge";

		args += " -node nodeB";
		args += " -pylon webSocket:pylonWebsocket isServer:localhost:8886";
		args += " -pylon WSRegions:pylonWSRegions isServer:localhost:8885";
		args += " -bridge interoperability:bridge1 in-context-of:webSocket:pylonWebsocket";

		args += " -node nodeA";
		args += " -pylon WSRegions:pylonA connectTo:localhost:8885";
		args += " -agent :ws://localhost:8885/agentA -shard messaging -shard ScriptTesting from:Simple";

		args += " -node nodeC";
		args += " -pylon webSocket:pylonC connectTo:ws://localhost:8886";
		args += " -agent :agentC -shard messaging";

		FlashBoot.main(args.split(" "));
	}
}