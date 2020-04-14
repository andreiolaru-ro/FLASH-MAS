package test.simplePingPong;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class Boot
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
		test_args += " -agent AgentA classpath:AgentPingPong otherAgent:AgentB";
		test_args += " -agent AgentB classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
