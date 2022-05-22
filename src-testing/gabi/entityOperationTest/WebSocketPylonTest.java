package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.entities.Node;
import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.model.OperationCall;

import java.util.ArrayList;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.Node.NODE_NAME;
import static net.xqhs.flash.ent_op.entities.ReceiveOperation.RECEIVE_OPERATION_NAME;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_PYLON_CONFIG;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_PYLON_NAME;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_SERVER_PORT_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class WebSocketPylonTest {
    public static void main(String[] args) throws InterruptedException {
        // first node setup
        Node node1 = new Node();
        node1.setup(new MultiTreeMap().addFirstValue(NODE_NAME, "node1"));

        // add pylon
        WebSocketPylon pylon1 = new WebSocketPylon();
        node1.addEntity(pylon1, new MultiTreeMap()
                .addSingleTree(WEBSOCKET_PYLON_CONFIG, new MultiTreeMap()
                        .addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                        .addSingleValue(WEBSOCKET_SERVER_PORT_NAME, "8885")
                        .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon1")
                        .addSingleValue(NODE_NAME, "node1")));
        // start node
        node1.start();

        // add agent
        Agent agent1 = new Agent();
        node1.addEntity(agent1, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "agent1")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "agent1"));

        // second node setup
        Node node2 = new Node();
        node2.setup(new MultiTreeMap().addFirstValue(NODE_NAME, "node2"));

        // add pylon
        WebSocketPylon pylon2 = new WebSocketPylon();
        node2.addEntity(pylon2, new MultiTreeMap()
                .addSingleTree(WEBSOCKET_PYLON_CONFIG, new MultiTreeMap()
                        .addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                        .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon2")
                        .addSingleValue(NODE_NAME, "node2")));

        //start node
        node2.start();

        Agent agent2 = new Agent();
        node2.addEntity(agent2, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "agent2")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "agent2"));

        // agent1(node1) calls an agent2(node2) operation
        ArrayList<Object> argumentValues = new ArrayList<>();
        argumentValues.add("simple message");
        OperationCall operationCall = new OperationCall(agent1.getEntityID(), agent2.getEntityID(), RECEIVE_OPERATION_NAME, false, argumentValues);
        agent1.callOperation(operationCall);

        // stop nodes
        Thread.sleep(10000);
        node1.stop();
        node2.stop();
    }
}
