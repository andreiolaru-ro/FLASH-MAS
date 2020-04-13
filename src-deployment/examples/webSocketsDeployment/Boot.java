package examples.webSocketsDeployment;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class Boot
{
	/**
	 * Performs test.a
	 * 
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";
		
		test_args += " -package examples.simplePingPong";
		test_args += " -node node1";
		test_args += " -support websocket:slave1 connectTo:ws://localhost:8885 classpath:websockets.WebSocketPylon";
		test_args += " -agent AgentA classpath:examples.simplePingPong.AgentPingPong otherAgent:AgentB";
		
		test_args += " -node node2";
		test_args += " -support websocket:slave2 serverPort:8885 connectTo:ws://localhost:8885 classpath:websockets.WebSocketPylon";
		test_args += " -agent AgentB classpath:examples.simplePingPong.AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
