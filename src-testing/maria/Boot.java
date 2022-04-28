package maria;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;

public class Boot {
    public static void main(String[] args) {
        String test_args = "";
		test_args += " -package maria example.compositePingPong -loader agent:composite";

        test_args += " -node nodeA";
        test_args += " -pylon webSocket:slaveA serverPort:8888 connectTo:ws://localhost:8888";
		test_args += " -agent agentA1 classpath:maria.MobileCompositeAgent -shard messaging -shard MonitoringTestShard";
		test_args += " -agent agentA2 -shard messaging -shard MonitoringTestShard";

        test_args += " -node nodeB";
        test_args += " -pylon webSocket:slaveB connectTo:ws://localhost:8888";
		test_args += " -agent agentB1 -shard messaging";


        List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(test_args.split(" ")));
        for(Node node : nodes) {
            node.start();
            System.out.println(node.getName() + " has the following entities: " + node.entityOrder);
        }

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MobileCompositeAgent agentA1 = (MobileCompositeAgent) getAgent(nodes, "agentA1");

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        agentA1.moveTo("nodeB");

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Node node : nodes) {
            System.out.println(node.getName() + " has the following entities: " + node.entityOrder);
//            node.stop();
        }
    }

    private static Entity<?> getAgent(List<Node> nodes, String entityName) {
        for (Node node: nodes) {
            for (Entity<?> entity: node.entityOrder) {
                if (entity.getName().contentEquals(entityName)) {
                    return entity;
                }
            }
        }

        return null;
    }
}