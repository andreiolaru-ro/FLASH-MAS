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
		
		args += " -package test.simplePingPong wsRegions";
		
		args += " -node nodeA-localhost:8885";
		args += " -pylon WSRegions:pylonA1 isServer:localhost:8885";
		args += " -pylon webSocket:pylonA2 serverPort:8886";
		args += " -agent agentA-localhost classpath:BridgeAgent ping:agentD pong:agentB-localhost:8885 -shard messaging -shard EchoTesting exit:30";
		
		args += " -node nodeB-localhost:8885";
		args += " -pylon WSRegions:pylonB connectTo:localhost:8885";
		args += " -agent composite:agentB-localhost:8885 -shard messaging -shard EchoTesting exit:30";
		
		args += " -node nodeD";
		args += " -pylon webSocket:pylonD connectTo:ws://localhost:8886";
		args += " -agent agentD classpath:AgentPingPong sendTo:agentA-localhost -shard EchoTesting exit:30";
		
		FlashBoot.main(args.split(" "));
	}
}