package test.compositeMobility;

import java.util.Arrays;
import java.util.List;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;

/**
 * Main class
 * 
 * @author Andrei Olaru
 */
public class Boot {
	
	/**
	 * Tested functionality.
	 */
	public static final String FUNCTIONALITY = "MOBILITY";
	/**
	 * If <code>true</code>, the mobile agent will ping the other two agents. If <code>false</code>, another agent pings
	 * the mobile agent.
	 */
	static final boolean MOBILE_AGENT_PINGS = true;
	/**
	 * Port for the WebSocket server.
	 */
	static final String			WS_PORT				= Integer.valueOf(8988).toString();
	
	/**
	 * Arguments for creating agents on nodeA.
	 */
	public static String	nodeA_agents	= "";
	/**
	 * Arguments for creating agents on nodeB.
	 */
	public static String	nodeB_agents	= "";
	/**
	 * Arguments for packages and loaders.
	 */
	public static String	prelude			= " -package testing test.compositeMobility -loader agent:composite -loader agent:mobileComposite";
	
	static {
		nodeA_agents += " -agent mobileComposite:agentA1 -shard messaging -shard EchoTesting -shard MobilityTest to:nodeB time:5000";
		nodeA_agents += MOBILE_AGENT_PINGS ? " -shard PingTest every:500 otherAgent:agentB1 otherAgent:agentA2"
				: " -shard PingBackTest";
		nodeA_agents += " -agent agentA2 -shard messaging -shard EchoTesting -shard PingBackTest";
		
		nodeB_agents += " -agent agentB1 -shard messaging -shard EchoTesting";
		nodeB_agents += MOBILE_AGENT_PINGS ? " -shard PingBackTest"
				: " -shard PingTest otherAgent:agentA1 otherAgent:agentA2";
	}
	
	/**
	 * Main
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		String a = "";
		a += prelude;
		
		a += " -node nodeA";
		a += " -pylon webSocket:pylonA serverPort:" + WS_PORT;
		a += nodeA_agents;
		
		a += " -node nodeB";
		a += " -pylon webSocket:pylonB connectTo:ws://localhost:" + WS_PORT;
		a += nodeB_agents;
		
		List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(a.split(" ")));
		nodes.forEach(node -> node.start());
	}
}