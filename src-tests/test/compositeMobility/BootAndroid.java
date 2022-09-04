package test.compositeMobility;

import java.util.Arrays;
import java.util.List;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.flash.FlashBoot;

/**
 * Main class
 *
 * @author Toni Gancea
 */
public class BootAndroid {

    /**
     * Tested functionality.
     */
    public static final String FUNCTIONALITY = "MOBILITY";

    /**
     * If <code>true</code>, the mobile agent will ping the other two agents. If <code>false</code>, another agent pings
     * the mobile agent.
     */
    public static final boolean MOBILE_AGENT_PINGS = true;

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
    public static String prelude	= " -package test.compositeMobility test.compositePingPong -loader agent:composite -loader agent:mobileComposite";

    static {
        nodeA_agents += " -agent mobileComposite:agentA1 -shard messaging -shard MonitoringTest -shard MobilityTest to:nodeB time:5000";
        nodeA_agents += MOBILE_AGENT_PINGS ? " -shard PingTest every:1000 otherAgent:agentB1 otherAgent:agentA2"
                : " -shard PingBackTest";
        nodeA_agents += " -agent agentA2 -shard messaging -shard MonitoringTest -shard PingBackTest";

//        ** second node + agent is deployed from the android device **
//        nodeB_agents += " -agent agentB1 -shard messaging -shard MonitoringTest";
//        nodeB_agents += MOBILE_AGENT_PINGS ? " -shard PingBackTest"
//                : " -shard PingTest otherAgent:agentA1 otherAgent:agentA2";
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
        a += " -pylon webSocket:pylonA serverPort:8888";
        a += nodeA_agents;

//        a += " -node nodeB";
//        a += " -pylon webSocket:pylonB connectTo:ws://localhost:8888";
//        a += nodeB_agents;

//        List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(a.split(" ")));
//        nodes.forEach(node -> node.start());
        FlashBoot.main(a.split(" "));
    }
}