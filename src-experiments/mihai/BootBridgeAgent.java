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
		
		args += " -package testing wsRegions src-experiments.mihai -loader agent:mobileComposite";
		
		args += " -node nodeA-localhost:8885";
		args += " -pylon WSRegions:pylonA isServer:localhost:8885";
		// args += " -agent :agentA-localhost:8885 -shard messaging -shard EchoTesting -shard PingTest
		// otherAgent:agentD-localhost:8885";
		args += " -agent :agentA-localhost:8885 -shard messaging  -shard EchoTesting exit:20 -shard ScriptTesting from:Simple";
		
		args += " -node nodeB-localhost:8885";
		args += " -pylon WSRegions:pylonB1 connectTo:localhost:8885";
		// args += " -pylon webSocket:pylonB2 serverPort:8886";
		args += " -agent :agentB-localhost:8885 -shard messaging -shard EchoTesting exit:20";
		
		args += " -node nodeC-localhost:8885";
		args += " -pylon WSRegions:pylonC connectTo:localhost:8885";
		args += " -agent :agentC-localhost:8885 -shard messaging -shard EchoTesting exit:20";
		
		args += " -node nodeD-localhost:8885";
		args += " -pylon WSRegions:pylonD connectTo:localhost:8885";
		// args += " -agent :agentD-localhost:8885 -shard messaging -shard EchoTesting -shard PingBackTest";
		args += " -agent :agentD-localhost:8885 -shard messaging -shard EchoTesting exit:20 -shard ScriptTesting from:Simple";
		
		// MasterLog.enablePerformanceModeTools(500);
		// System.out.println("."); // to activate console output.
		FlashBoot.main(args.split(" "));
	}
}