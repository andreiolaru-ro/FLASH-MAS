package andrei.deploymentTest;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class NodesTest
{
	/**
	 * Performs test.
	 * 
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";
		
		test_args += " -loader agent classpath:core.composite.CompositeAgentLoader";
		test_args += " -node nodeA -agent agentA1 -agent agentA2 -agent agentA3";
		test_args += " -node nodeB -agent agentB1 -agent agentB2 -agent agentB3";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
