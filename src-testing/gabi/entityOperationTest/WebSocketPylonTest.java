package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.agent.Agent;
import net.xqhs.flash.ent_op.entities.Node;
import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.entities.agent.ComputingAgent;
import net.xqhs.flash.ent_op.model.OperationCallWave;

import java.util.ArrayList;
import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.Node.NODE_NAME;
import static net.xqhs.flash.ent_op.entities.operations.MultiplyOperation.MULTIPLY_OPERATION_NAME;
import static net.xqhs.flash.ent_op.entities.operations.ReceiveOperation.RECEIVE_OPERATION_NAME;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_PYLON_CONFIG;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_PYLON_NAME;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME;
import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_SERVER_PORT_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class WebSocketPylonTest {
    public static void main(String[] args) throws InterruptedException {

        // ******************************** first node setup ************************************************ //
        Node node1 = new Node();
        node1.setup(new MultiTreeMap().addFirstValue(NODE_NAME, "node1"));

        WebSocketPylon pylon = new WebSocketPylon();
        node1.addEntity(pylon, new MultiTreeMap()
                .addSingleTree(WEBSOCKET_PYLON_CONFIG, new MultiTreeMap()
                        .addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                        .addSingleValue(WEBSOCKET_SERVER_PORT_NAME, "8885")
                        .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon1")
                        .addSingleValue(NODE_NAME, "node1")));

        for (int i = 0; i < 6; i++) {
            WebSocketPylon pylon_ = new WebSocketPylon();
            node1.addEntity(pylon_, new MultiTreeMap()
                    .addSingleTree(WEBSOCKET_PYLON_CONFIG, new MultiTreeMap()
                            .addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                            .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon1." + i)
                            .addSingleValue(NODE_NAME, "node1")));
        }
        // start node
        node1.start();

        // add agent
        Agent agent1 = new Agent();
        node1.addEntity(agent1, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "ws://localhost:agent1")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent1"));

        // ******************************** second node setup ************************************************ //

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

        // add agent
        Agent agent2 = new Agent();
        node2.addEntity(agent2, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "ws://localhost:agent2")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent2"));

        // add agent
        Agent agent3 = new ComputingAgent();
        node2.addEntity(agent3, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "ws://localhost:agent3")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent3"));

        // *************************************** op call *************************************************** //

        // agent1(node1) calls an agent3(node2) operation
        List<Object> argumentValues = new ArrayList<>();
        argumentValues.add("simple message");
        OperationCallWave operationCall = new OperationCallWave(agent1.getEntityID(), agent2.getEntityID(), RECEIVE_OPERATION_NAME, false, argumentValues);
        agent1.callOperation(operationCall);

        // agent1(node1) calls an agent3(node2) operation
        OperationCallWave multiplyOpCall = new OperationCallWave(agent1.getEntityID(), agent3.getEntityID(), MULTIPLY_OPERATION_NAME, true, List.of(1.3d, 24.5d, 65.8d));
        agent1.callOperation(multiplyOpCall);

        // stop nodes
        Thread.sleep(10000);
        node1.stop();
        node2.stop();
    }
}
