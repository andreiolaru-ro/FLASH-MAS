package test.deployment.convergence;

import net.xqhs.flash.FlashBoot;

/**
 * Test 3: full deployment in XML, additional parameters added through CLI for same agents.
 */

public class Boot3
{
    public static void main(String[] args)
	{
		String test_args = "src-tests/test/deployment/convergence/deployment3.xml";
		
        test_args += " -package testing";
        test_args += " -agent AgentA ping-number:3";
		test_args += " -agent AgentC ping-number:3";
		
		FlashBoot.main(test_args.split(" "));
	}
}
