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
	
	/**
	 * Main method.
	 * 
	 * @param args_
	 */
	public static void main(String[] args_) {
		String args = "";
		
		// Uncomment one of these lines:
		args = version1();
		// args = THIS_DIRECTORY + "complexDeployment.xml";
		// args = THIS_DIRECTORY + "complexDeployment-autonode.xml";
		FlashBoot.main(args.split(" "));
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
