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
		
		args += " -package testing wsRegions src-experiments.mihai -loader agent:BridgeAgent -loader agent:composite";
		
		args += " -node nodeA-localhost:8885";
		args += " -pylon WSRegions:pylonA1 isServer:localhost:8885";
		args += " -pylon webSocket:pylonA2 serverPort:8886";
		args += " -agent BridgeAgent:agentA-localhost:8885 ping:nodeD pong:nodeB-localhost:8885 -shard EchoTesting exit:40";
		
		args += " -node nodeB-localhost:8885";
		args += " -pylon WSRegions:pylonB connectTo:localhost:8885";
		args += " -agent composite:agentB-localhost:8885 -shard messaging -shard EchoTesting exit:40";
		
		args += " -node nodeD";
		args += " -pylon webSocket:pylonD connectTo:ws://localhost:8886";
		args += " -agent agentD classpath:AgentPingPong sendTo:nodeA-localhost:8885 -shard EchoTesting exit:40";
		
		FlashBoot.main(args.split(" "));
	}
}