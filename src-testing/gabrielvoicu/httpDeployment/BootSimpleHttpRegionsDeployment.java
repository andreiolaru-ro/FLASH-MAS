package gabrielvoicu.httpDeployment;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;

import java.util.Arrays;
import java.util.List;

public class BootSimpleHttpRegionsDeployment {

    /**
     * Arguments for creating agents on nodeA.
     */
    public static String	nodeA_agents	= "";
    /**
     * Arguments for creating agents on nodeB.
     */
    public static String	nodeB_agents	= "";

    public static String prelude = "-package httphomeserver -package testing test.compositeMobility -loader agent:composite -loader agent:mobileComposite";

    static {
        nodeA_agents += " -agent mobileComposite:http://localhost:8885/agentA1 -shard messaging -shard EchoTesting -shard MobilityTest to:http://localhost:8886/nodeB time:5000";
        nodeA_agents += " -shard PingTest every:500 otherAgent:http://localhost:8886/agentB1 otherAgent:http://localhost:8885/agentA2";
        nodeA_agents += " -agent :http://localhost:8885/agentA2 -shard messaging -shard EchoTesting -shard PingTest every:500 otherAgent:http://localhost:8885/agentA1";
        nodeB_agents += " -agent :http://localhost:8886/agentB1 -shard messaging -shard EchoTesting";
        nodeB_agents += " -shard PingBackTest";
    }

    public static void main(String[] args_) {
        String a = "";
        a += prelude;

        a += " -node http://localhost:8885/nodeA";
        a += " -pylon RegionsHttp:pylonA serverPort:8885 connectTo:http://localhost:8886/agentB1 connectTo:http://localhost:8886/nodeB";
        a += nodeA_agents;

        a += " -node http://localhost:8886/nodeB";
        a += " -pylon RegionsHttp:pylonB serverPort:8886 connectTo:http://localhost:8885/agentA1";
        a += nodeB_agents;

        List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(a.split(" ")));
        nodes.forEach(node -> node.start());

    }
}
