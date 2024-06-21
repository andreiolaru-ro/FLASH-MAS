package mihai;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootWebSocketToWSRegions {
	/**
	 * Performs test
	 * 
	 * @param args_ - not used.
	 */

	public static void main(String[] args_) {
		String args = "";
		args += " -package wsRegions mihai -loader agent:composite -load_order pylon;agent;bridge";
		System.out.println("hello");

		if (args_[0].equals("1")) {
			System.out.println("hello");
			args += " -node nodeB keep:200";
			args += " -pylon webSocket:pylonWebsocket isServer:10.89.51.225:8886";
			args += " -pylon WSRegions:pylonWSRegions isServer:10.89.51.225:8885";
			args += " -bridge interoperability:bridge1 in-context-of:webSocket:pylonWebsocket";
		}

		if (args_[0].equals("2")) {
			args += " -node nodeA";
			args += " -pylon WSRegions:pylonA connectTo:10.89.51.225:8885";
			args += " -agent :ws://10.89.51.225:8885/agentA -shard messaging -shard EchoTesting";
		}

		if (args_[0].equals("3")) {
			args += " -node nodeC";
			args += " -pylon webSocket:pylonC connectTo:ws://10.89.51.225:8886";
			args += " -agent agentC classpath:AgentPingPong sendTo:ws://10.89.51.225:8885/agentA";
		}

		FlashBoot.main(args.split(" "));
	}
}