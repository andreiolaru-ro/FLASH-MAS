package laura;

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
		
		test_args += " -package laura";
		test_args += " -support ros classpath:net.xqhs.flash.local.RosSupport";
		test_args += " -agent AgentA classpath:laura.AgentPingPong sendTo:AgentB";
		test_args += " -agent AgentB classpath:laura.AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
