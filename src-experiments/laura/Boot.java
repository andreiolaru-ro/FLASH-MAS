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
		
//		test_args += " -package laura";
//		test_args += " -node main";
//		test_args += " -support ros classpath:net.xqhs.flash.ros.RosSupport connect-to:ws://localhost:9090";
//		test_args += " -agent AgentA classpath:PingPongAgent sendTo:AgentB";
//		test_args += " -agent AgentB classpath:PingPongAgent";

		test_args += " -package laura";
		test_args += " -node nodeA";
		test_args += " -support ros classpath:net.xqhs.flash.ros.RosSupport connect-to:ws://localhost:9090";
		test_args += " -agent agentA1 classpath:PingPongAgent sendTo:agentA2 sendTo:agentB2";
		test_args += " -agent agentA2 classpath:PingPongAgent";
		test_args += " -node nodeB";
		test_args += " -support ros classpath:net.xqhs.flash.ros.RosSupport connect-to:ws://localhost:9090";
		test_args += " -agent agentB1 classpath:PingPongAgent sendTo:agentB2";
		test_args += " -agent agentB2 classpath:PingPongAgent";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
