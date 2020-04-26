package test.simplePingPong;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class Boot
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
		
		test_args += " -package test.simplePingPong";
		test_args += " -node node1";
		test_args += " -agent AgentA classpath:test.simplePingPong.AgentPingPong sendTo:AgentB";
		test_args += " -agent AgentB classpath:test.simplePingPong.AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
