package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.entities.Node;
import net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;

import java.util.ArrayList;
import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.Node.NODE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.MultiplyOperation.MULTIPLY_OPERATION_NAME;
import static net.xqhs.flash.ent_op.impl.operations.ReceiveOperation.RECEIVE_OPERATION_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_PYLON_CONFIG;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_PYLON_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_SERVER_PORT_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class WebSocketPylonTest {
    public static void main(String[] args) throws InterruptedException {

        // ******************************** first node setup ************************************************ //
        var node1 = new Node();
        node1.setup(new MultiTreeMap().addFirstValue(NODE_NAME, "node1"));

        var pylon = new WebSocketPylon();
        node1.addEntity(pylon, new MultiTreeMap()
                .addSingleTree(WEBSOCKET_PYLON_CONFIG, new MultiTreeMap()
                        .addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                        .addSingleValue(WEBSOCKET_SERVER_PORT_NAME, "8885")
                        .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon1")
                        .addSingleValue(NODE_NAME, "node1")));

        for (int i = 0; i < 6; i++) {
            var pylon_ = new WebSocketPylon();
            node1.addEntity(pylon_, new MultiTreeMap()
                    .addSingleTree(WEBSOCKET_PYLON_CONFIG, new MultiTreeMap()
                            .addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                            .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon1." + i)
                            .addSingleValue(NODE_NAME, "node1")));
        }
        // start node
        node1.start();

        // add agent
        var agent1 = new Agent();
        node1.addEntity(agent1, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "ws://localhost:agent1")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent1"));

        // ******************************** second node setup ************************************************ //

        var node2 = new Node();
        node2.setup(new MultiTreeMap().addFirstValue(NODE_NAME, "node2"));

        // add pylon
        var pylon2 = new WebSocketPylon();
        node2.addEntity(pylon2, new MultiTreeMap()
                .addSingleTree(WEBSOCKET_PYLON_CONFIG, new MultiTreeMap()
                        .addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                        .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon2")
                        .addSingleValue(NODE_NAME, "node2")));
        // start node
        node2.start();

        // add agent
        var agent2 = new Agent();
        node2.addEntity(agent2, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "ws://localhost:agent2")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent2"));

        // add agent
        var agent3 = new ComputingAgent();
        node2.addEntity(agent3, new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "ws://localhost:agent3")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent3"));

        // *************************************** op call *************************************************** //

        // agent1(node1) calls an agent3(node2) operation
        List<Object> argumentValues = new ArrayList<>();
        argumentValues.add("simple message");
        var operationCall = new OperationCallWave(agent1.getEntityID(), agent2.getEntityID(), RECEIVE_OPERATION_NAME, false, argumentValues);
        agent1.callOperation(operationCall);

        // agent1(node1) calls an agent3(node2) operation
        var multiplyOpCall = new OperationCallWave(agent1.getEntityID(), agent3.getEntityID(), MULTIPLY_OPERATION_NAME, true, List.of(1.3d, 24.5d, 65.8d));
        agent1.callOperationWithResult(multiplyOpCall, x -> System.out.println("The result of the multiply operation is " + x));

        // agent1(node1) creates a relation with agent2(node2)
        var relation =  new Relation(agent1.getEntityID(), agent2.getEntityID(), "agent1-agent2-relation");
        agent1.callRelationChange(Relation.RelationChangeType.CREATE, relation);

        // stop nodes
        Thread.sleep(10000);
        node1.stop();
        node2.stop();
    }
}
