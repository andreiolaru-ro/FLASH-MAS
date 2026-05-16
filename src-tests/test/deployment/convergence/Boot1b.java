package test.deployment.convergence;

import net.xqhs.flash.FlashBoot;

/**
 * Test 1b: node and pylon in XML, agents added via CLI with explicit in-context-of.
 * <p>
 * Expected behavior: agents are placed in the pylon specified by the in-context-of parameter.
 * AgentA sends 5 ping messages to AgentB, which replies to each. Both agents stop after 
 * the ping limit is reached.
*/

public class Boot1b
{
    public static void main(String[] args)
	{
		String test_args = "src-tests/test/deployment/convergence/deployment1.xml";
		
        test_args += " -package testing";
		test_args += " -agent PingPong:AgentA classpath:AgentPingPong sendTo:AgentB in-context-of:def";
		test_args += " -agent PingPong:AgentB classpath:AgentPingPong in-context-of:def";
		
		FlashBoot.main(test_args.split(" "));
	}
}
