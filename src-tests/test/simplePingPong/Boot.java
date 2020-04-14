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
		
		test_args += " -package examples.simplePingPong";
		test_args += " -agent AgentA classpath:examples.simplePingPong.AgentPingPong otherAgent:AgentB";
		test_args += " -agent AgentB classpath:examples.simplePingPong.AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
