package daniel;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.flash.webSocket.WebSocketServerEntity;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MobilityTest {
    public static void main(String[] args) {
        String test_args = "";
        test_args += " -package test.mobility -loader agent:composite";

        test_args += " -node nodeA";
        test_args += " -pylon webSocket:slaveA serverPort:8886 connectTo:ws://localhost:8886";
        test_args += " -agent composite:agentA1 -shard messaging -shard CountShard";

        test_args += " -node nodeB";
        test_args += " -pylon webSocket:slaveB serverPort:8886 connectTo:ws://localhost:8886";
        test_args += " -agent composite:agentB1 -shard messaging -shard CountShard";

        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);

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

        CompositeAgent agentA1 = (CompositeAgent) getAgent(nodes, "agentA1");
        agentA1.prepareForMove();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        agentA1.moveTo("agentB1");

        try {
            TimeUnit.SECONDS.sleep(7);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Node node : nodes) {
            System.out.println(node.getName() + " has the following entities: " + node.entityOrder);
            node.stop();
        }
    }

    private static String serialize(CompositeAgent agent) {
        ByteArrayOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(fos);
            out.writeObject(agent);
            out.close();
            System.out.println("The byte array is: ");
            System.out.println(fos);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String objStringBase64 = Base64.getEncoder().encodeToString(fos.toByteArray());
        return objStringBase64;
    }

    private static Entity getAgent(List<Node> nodes, String entityName) {
        for (Node node: nodes) {
            for (Entity entity: node.entityOrder) {
                if (entity.getName().contentEquals(entityName)) {
                    return entity;
                }
            }
        }

        return null;
    }
}
