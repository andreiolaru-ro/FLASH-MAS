package test.deployment.convergence;

import net.xqhs.flash.FlashBoot;

/**
 * Test 1b: node and pylon in XML, agents added via CLI with explicit in-context-of.
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
