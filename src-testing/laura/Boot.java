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
		test_args += " -node main";
		test_args += " -support ros classpath:net.xqhs.flash.ros.RosSupport connect-to:ws://localhost:9090";
		test_args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
		test_args += " -agent AgentB classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
