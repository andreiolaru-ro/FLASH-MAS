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
//		test_args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
//		test_args += " -agent AgentB classpath:AgentPingPong";

		test_args += " -package laura";
		test_args += " -node nodeA";
		test_args += " -support ros classpath:net.xqhs.flash.ros.RosSupport connect-to:ws://localhost:9090";
		test_args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2";
		test_args += " -agent agentA2 classpath:AgentPingPong";
		test_args += " -node nodeB";
		test_args += " -support ros classpath:net.xqhs.flash.ros.RosSupport connect-to:ws://localhost:9090";
		test_args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2";
		test_args += " -agent agentB2 classpath:AgentPingPong";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}
