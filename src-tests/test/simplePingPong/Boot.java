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
		test_args += " -node main -pylon local:main use-thread";
		test_args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
		test_args += " -agent AgentB classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
