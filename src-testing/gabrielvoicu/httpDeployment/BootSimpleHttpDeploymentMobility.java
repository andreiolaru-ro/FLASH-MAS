package gabrielvoicu.httpDeployment;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;

import java.util.Arrays;
import java.util.List;

public class BootSimpleHttpDeploymentMobility {


    /**
     * Arguments for creating agents on nodeA.
     */
    public static String	nodeA_agents	= "";
    /**
     * Arguments for creating agents on nodeB.
     */
    public static String	nodeB_agents	= "";
    
    public static String	prelude			= " -package testing test.compositeMobility -loader agent:composite -loader agent:mobileComposite";

    static {
        nodeA_agents += " -agent agent:agentA1 classpath:AgentPingPong to:nodeB time:5000";
        nodeA_agents += " -shard PingTest every:500 otherAgent:agentB1 otherAgent:agentA2";
        nodeA_agents += " -agent agentA2 -shard messaging -shard EchoTesting -shard PingBackTest";

        nodeB_agents += " -agent agentB1 -shard messaging -shard EchoTesting";
        nodeB_agents += " -shard PingBackTest";
    }
    public static void main(String[] args_)
    {
        String a = "";
        a += prelude;

        a += " -node nodeA";
        a += " -pylon http:pylonA serverPort:8885";
        a += nodeA_agents;

        a += " -node nodeB";
        a += " -pylon http:pylonB serverPort:8886 connectTo:http://localhost:8885/agentA1";
        a += nodeB_agents;

        List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(a.split(" ")));
        nodes.forEach(node -> node.start());
        
    }
}
