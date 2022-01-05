package example.deployment;

import net.xqhs.flash.FlashBoot;

/**
 * Use this class to start some example configurations.
 * <p>
 * Some of them are just examples of how to specify a configuration and don't actually do anything.
 * 
 * @author Andrei Olaru
 */
public class BootExample {
	
	/**
	 * This directory.
	 */
	static String THIS_DIRECTORY = "src-examples/example/deployment/";
	
	static int	bitcounter		= 0;
	static int	DEFINE_AGENT_A	= 1 << bitcounter++;
	static int	DEFINE_AGENT_B	= 1 << bitcounter++;
	static int	DEFINE_PYLON	= 1 << bitcounter++;
	static int	DEFINE_NODE		= 1 << bitcounter++;
	
	/**
	 * Main method.
	 * 
	 * @param args_
	 */
	public static void main(String[] args_) {
		String args = "";
		
		// Uncomment one of these lines:
		args = version0(0
				
				| DEFINE_AGENT_A
				
				| DEFINE_AGENT_B
				
				// | DEFINE_PYLON
				
				| DEFINE_NODE
		
		);
		// args = version1();
		// args = THIS_DIRECTORY + "complexDeployment.xml";
		// args = THIS_DIRECTORY + "complexDeployment-autonode.xml";
		FlashBoot.main(args.split(" "));
	}
	
	/**
	 * @param mask
	 *            see the static definitions.
	 * @return a customizable configuration of a simple scenario.
	 */
	public static String version0(int mask) {
		String args = "";
		if((mask & (DEFINE_AGENT_A | DEFINE_AGENT_B)) > 0)
			args += " -package example.simplePingPong";
		if((mask & DEFINE_NODE) > 0)
			args += " -node node1";
		if((mask & DEFINE_PYLON) > 0)
			args += " -pylon local:main";
		if((mask & DEFINE_AGENT_A) > 0)
			args += " -agent AgentA classpath:AgentPingPong sendTo:AgentB";
		if((mask & DEFINE_AGENT_B) > 0)
			args += " -agent AgentB classpath:AgentPingPong";
		return args;
	}
	
	/**
	 * @return a configuration which is entirely specified via CLI arguments.
	 */
	public static String version1() {
		String args = "";
		args += " -package test.simplePingPong";
		args += " -loader agent:composite";
		args += " -pylon websocket";
		args += " -node A";
		args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2";
		args += " -agent agentA2 classpath:AgentPingPong";
		args += " -node B";
		args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2 sendTo:agentA1";
		args += " -agent agentB2 classpath:AgentPingPong";
		
		return args;
	}
}
