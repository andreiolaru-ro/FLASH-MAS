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
	
	@SuppressWarnings("javadoc")
	static int	bitcounter				= 0;
	/**
	 * Include the sending agent in version0 scenario.
	 */
	static int	DEFINE_AGENT_A			= 1 << bitcounter++;
	/**
	 * Include the receiving agent in version0 scenario.
	 */
	static int	DEFINE_AGENT_B			= 1 << bitcounter++;
	/**
	 * Include a local pylon definition in version0 scenario.
	 */
	static int	DEFINE_LOCAL_PYLON		= 1 << bitcounter++;
	/**
	 * Include a local pylon definition in version0 scenario.
	 */
	static int	DEFINE_WEBSOCKET_PYLON	= 1 << bitcounter++;
	/**
	 * Include the node definition in version0 scenario.
	 */
	static int	DEFINE_NODE				= 1 << bitcounter++;
	
	/**
	 * Main method.
	 * 
	 * @param args_
	 */
	public static void main(String[] args_) {
		String args = "";
		
		// Uncomment one of these lines:
		// just the agents, the node and the pylon will be automatically added.
		args = version0(DEFINE_AGENT_A | DEFINE_AGENT_B);
		// args = THIS_DIRECTORY + "simpleDeployment.xml"; // equivalent to above
		// // the node will be automatically added
		// args = version0(DEFINE_AGENT_A | DEFINE_AGENT_B | DEFINE_LOCAL_PYLON);
		// // the node will be automatically added
		// args = version0(DEFINE_AGENT_A | DEFINE_AGENT_B | DEFINE_WEBSOCKET_PYLON);
		// // the pylon will be automatically added
		// args = version0(DEFINE_AGENT_A | DEFINE_AGENT_B | DEFINE_NODE);
		// // everything is explicitely defined in the scenario
		// args = version0(DEFINE_AGENT_A | DEFINE_AGENT_B | DEFINE_NODE | DEFINE_LOCAL_PYLON);
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
		if((mask & DEFINE_LOCAL_PYLON) > 0)
			args += " -pylon local:main";
		if((mask & DEFINE_WEBSOCKET_PYLON) > 0)
			args += " -pylon webSocket:ws serverPort:8886";
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
		args += " -package example.simplePingPong";
		args += " -loader agent:composite";
		args += " -pylon webSocket:ws connectTo:ws://localhost:8886";
		args += " -node A";
		args += " -pylon webSocket:wsMain serverPort:8886";
		args += " -agent agentA1 classpath:AgentPingPong sendTo:agentA2 sendTo:agentB2 sendTo:agentC2";
		args += " -agent agentA2 classpath:AgentPingPong";
		args += " -node B";
		args += " -agent agentB1 classpath:AgentPingPong sendTo:agentB2 sendTo:agentA1 sendTo:agentC2";
		args += " -agent agentB2 classpath:AgentPingPong";
		args += " -node C";
		args += " -agent agentC1 classpath:AgentPingPong sendTo:agentB2 sendTo:agentA1 sendTo:agentB2";
		args += " -agent agentC2 classpath:AgentPingPong";
		
		return args;
	}
}
