package mihai;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootSimple {
	/**
	 * Performs test
	 * 
	 * @param args_ - not used.
	 */
	public static void main(String[] args_) {
		String args = "";

		args += " -package testing wsRegions -loader agent:composite";

		args += " -node nodeA";
		args += " -pylon webSocket:pylonA serverPort:8886";
		args += " -agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentD -shard EchoTesting";

		args += " -node nodeB";
		args += " -pylon webSocket:pylonB connectTo:ws://localhost:8886";
		args += " -agent composite:AgentA -shard messaging -shard EchoTesting";

		args += " -node nodeC-localhost:8885";
		args += " -pylon WSRegions:pylonC isServer:localhost:8885";
		args += " -agent composite:agentC-localhost:8885 -shard messaging -shard EchoTesting";
		
		args += " -node nodeD-localhost:8885";
		args += " -pylon WSRegions:pylonD connectTo:localhost:8885";
		args += " -agent composite:agentD-localhost:8885 -shard messaging -shard PingBackTest -shard EchoTesting";

		FlashBoot.main(args.split(" "));
	}
}