package test.deployment.convergence;

import net.xqhs.flash.FlashBoot;

/**
 * Test 2: node in CLI, pylon and agents in XML with in-context-of.
 * <p>
 * Expected behavior: the pylon defined in XML with in-context-of pointing 
 * to the CLI-defined node is correctly placed under that node. AgentA 
 * sends 5 ping messages to AgentB, which replies to each. Both agents 
 * stop after the ping limit is reached.
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
