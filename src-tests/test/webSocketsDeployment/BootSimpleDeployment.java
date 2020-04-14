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
		
		test_args += " -package test.simplePingPong";
		test_args += " -node node1";
		test_args += " -pylon webSocket:slave1 serverPort:8886 connectTo:ws://localhost:8886";
		test_args += " -agent AgentA classpath:AgentPingPong otherAgent:AgentB";
		
		test_args += " -node node2";
		test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8886";
		test_args += " -agent AgentB classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
