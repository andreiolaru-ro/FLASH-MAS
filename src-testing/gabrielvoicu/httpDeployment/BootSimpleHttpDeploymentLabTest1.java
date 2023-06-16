package gabrielvoicu.httpDeployment;

import net.xqhs.flash.FlashBoot;
import net.xqhs.util.logging.MasterLog;

public class BootSimpleHttpDeploymentLabTest1 {

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
        int index = -1;
        if(args_.length > 0) {
            index = Integer.parseInt(args_[0]);
        }
        String[] pylonNames = {"A", "B", "C"};
//        String[] serverHosts = {"localhost", "localhost", "localhost"};
        String[] serverHosts = {"172.19.3.120", "172.19.3.51", "172.19.3.51"};
        String[] serverPorts = {"8885", "8886", "8887"};
        String[] nodeNames = { "http://" + serverHosts[0] + ":8885/nodeA", "http://" + serverHosts[1] + ":8886/nodeB", "http://" + serverHosts[2] + ":8887/nodeC" };
        String[] agentNames = { "http://" + serverHosts[0] + ":8885/agentA", "http://" + serverHosts[1] + ":8886/agentB", "http://" + serverHosts[2] + ":8887/agentC" };
        
        for(int i = index < 0 ? 0 : index; i < (index < 0 ? 3 : index + 1); i++) {
            a += " -node "  + nodeNames[i] + " -pylon RegionsHttp:Pylon" + pylonNames[i] + " serverHost:" + serverHosts[i] + " serverPort:" + serverPorts[i];
            for (int j = 0; j < 3; j++) {
                if (j == i) {
                    continue;
                }
                a += " connectTo:" + agentNames[j];
            }
            if (i > 0) {
                a += " -agent :" + agentNames[i];
            } else {
                a += " -agent mobileComposite:" + agentNames[i];
            }
            a += " -shard messaging -shard EchoTesting";
            if (i == 0) {
                a += " -shard MobilityTest to:" + nodeNames[1] + " time:5000 -shard PingTest every:500 otherAgent:" + agentNames[1] + " otherAgent:" + agentNames[2];
            }
            if (i == 1) {
                a += " -shard PingTest every:500 otherAgent:" + agentNames[0];
            }
            if (i == 2) {
                a += " -shard PingBackTest";
            }
        }

//        MasterLog.enablePerformanceModeTools(1000);
//        MasterLog.activateGlobalPerformanceMode();

        FlashBoot.main(a.split(" "));

    }
}
