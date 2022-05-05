package andrei.deploymentTest;

import net.xqhs.flash.FlashBoot;

/**
 * Use this class to start some example configurations.
 * 
 * @author Andrei Olaru
 */
public class NonStdEntitiesTest {
	
	/**
	 * Main method.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String test_args = "";
		
		test_args += " -package test.simplePingPong";
		test_args += " -loader agent:composite";
		test_args += " -node B";
		test_args += " -pylon websocket";
		test_args += " -agentgroup group1";
		test_args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2";
		test_args += " -agent agentA2 classpath:AgentPingPong";
		test_args += " -agentgroup group2";
		test_args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2 sendTo:agentA1";
		test_args += " -agent agentB2 classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
