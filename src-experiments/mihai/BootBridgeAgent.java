package mihai;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootBridgeAgent {
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */

	public static void main(String[] args_) {
		String args = "";

		// schimba ca numele agentB sa fie pentru BridgeAgent si sa se trimita de la A
		// la C
		args += " -package wsRegions mihai -loader agent:composite -load_order pylon;bridge;agent";

		args += " -node nodeB";
		args += " -pylon WSRegions:pylonB1 isServer:localhost:8885";
		args += " -pylon webSocket:pylonB2 isServer:localhost:8886";
		args += " -bridge interoperability: in-context-of:pylonB1";

		args += " -node nodeA";
		args += " -pylon WSRegions:pylonA connectTo:localhost:8885";
		args += " -agent :agentA -shard messaging";

		args += " -node nodeC";
		args += " -pylon webSocket:pylonC connectTo:ws://localhost:8886";
		args += " -agent agentC classpath:AgentPingPong sendTo:agentA";

		FlashBoot.main(args.split(" "));
	}
}