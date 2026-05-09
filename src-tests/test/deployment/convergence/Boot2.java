package test.deployment.convergence;

import net.xqhs.flash.FlashBoot;

/**
 * Test 2: node in CLI, pylon and agents in XML with in-context-of.
 */

public class Boot2
{
    public static void main(String[] args)
	{
		String test_args = "src-tests/test/deployment/convergence/deployment2.xml";
		
        test_args += " -package testing";
		test_args += " -node main";
		
		FlashBoot.main(test_args.split(" "));
	}
}
