package laura;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootSimple {
	/**
	 * Performs test.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		String test_args = "";
		
		test_args += " -package test.simplePingPong";
		
		// ce merge acum:
		test_args += " -node nodeA";
		test_args += " -support local classpath:net.xqhs.flash.local.LocalSupport";
		test_args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2";
		test_args += " -agent agentA2 classpath:AgentPingPong";
		test_args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2";
		test_args += " -agent agentB2 classpath:AgentPingPong";
		
		// ce vrem să meargă:
		// test_args += " -node nodeA";
		// test_args += " -support local classpath:net.xqhs.flash.local.LocalSupport";
		// test_args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2";
		// test_args += " -agent agentA2 classpath:AgentPingPong";
		// test_args += " -node nodeB";
		// test_args += " -support local classpath:net.xqhs.flash.local.LocalSupport";
		// test_args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2";
		// test_args += " -agent agentB2 classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
