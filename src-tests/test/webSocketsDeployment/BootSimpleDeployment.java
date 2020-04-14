package test.webSocketsDeployment;

import net.xqhs.flash.FlashBoot;

/**
 * Tests websockets support works in a deployment of non-composite agents.
 */
public class BootSimpleDeployment
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
		
		//test_args += " -package FLASH-MAS";
		test_args += " -node node1";
		test_args += " -support websocket:slave1 serverPort:8886 connectTo:ws://localhost:8886 classpath:websockets.WebSocketPylon";
		test_args += " -agent AgentA classpath:test.simplePingPong.AgentPingPong otherAgent:AgentB";
		
		test_args += " -node node2";
		test_args += " -support websocket:slave2 connectTo:ws://localhost:8886 classpath:websockets.WebSocketPylon";
		test_args += " -agent AgentB classpath:test.simplePingPong.AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
