package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.entities.Node;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.operations.RegisterOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Relation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.xqhs.flash.ent_op.impl.operations.MultiplyOperation.MULTIPLY_OPERATION;
import static net.xqhs.flash.ent_op.impl.operations.ReceiveOperation.RECEIVE_OPERATION;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.NODE_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_PYLON_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_SERVER_PORT_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class WebSocketPylonTest {
    public static void main(String[] args) throws InterruptedException {
        FMas fMas1 = new DefaultFMasImpl();

        // ******************************** first node setup ************************************************ //
        var node1 = new Node();
        node1.setup(new MultiTreeMap().addFirstValue(ENTITY_ID_ATTRIBUTE_NAME, "node1"));

        var pylon = new WebSocketPylon();
        pylon.setup(new MultiTreeMap().addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                .addSingleValue(WEBSOCKET_SERVER_PORT_NAME, "8885").addSingleValue(WEBSOCKET_PYLON_NAME, "pylon1")
                .addSingleValue(NODE_NAME, "node1"));
        fMas1.registerEntity(pylon);
        fMas1.registerEntity(node1);
        node1.addEntity(pylon);
        pylon.start();


        // for(int i = 0; i < 6; i++) {
        // var pylon_ = new WebSocketPylon();
        // pylon_.setup(new MultiTreeMap().addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
        // .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon1." + i).addSingleValue(NODE_NAME, "node1"));
        // fMas1.registerEntity(pylon_);
        // node1.addEntity(pylon_);
        // }

        // add agent
        var agent1 = new Agent();
        agent1.setup(new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent1"));
        fMas1.registerEntity(agent1);
        fMas1.route(new OperationCallWave(null, pylon.getID(), RegisterOperation.REGISTER_OPERATION, false,
                Arrays.asList(agent1.getID().ID)));
        node1.addEntity(agent1);

        // start node
        node1.start();
        // ******************************** second node setup ************************************************ //

        FMas fMas2 = new DefaultFMasImpl();

        var node2 = new Node();
        node2.setup(new MultiTreeMap().addFirstValue(ENTITY_ID_ATTRIBUTE_NAME, "node2"));
        fMas2.registerEntity(node2);

        // add pylon
        var pylon2 = new WebSocketPylon();
        pylon2.setup(new MultiTreeMap().addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                .addSingleValue(WEBSOCKET_PYLON_NAME, "pylon2").addSingleValue(NODE_NAME, "node2"));
        fMas2.registerEntity(pylon2);
        pylon2.start();
        node2.addEntity(pylon2);

        // add agent
        var agent2 = new Agent();
        agent2.setup(new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent2"));
        fMas2.registerEntity(agent2);
        node2.addEntity(agent2);
        fMas2.route(new OperationCallWave(null, pylon2.getID(), RegisterOperation.REGISTER_OPERATION, false,
                Arrays.asList(agent2.getID().ID)));

        // add agent
        var agent3 = new ComputingAgent();
        agent3.setup(new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "ws://localhost:agent3"));
        fMas2.registerEntity(agent3);
        fMas2.route(new OperationCallWave(null, pylon2.getID(), RegisterOperation.REGISTER_OPERATION, false,
                Arrays.asList(agent3.getID().ID)));
        node2.addEntity(agent3);

        // start node
        node2.start();
        // *************************************** op call *************************************************** //

        Thread.sleep(1000);

        // agent1(node1) calls an agent3(node2) operation
        List<Object> argumentValues = new ArrayList<>();
        argumentValues.add("simple message");
        var operationCall = new OperationCallWave(agent1.getID(), agent2.getID(), RECEIVE_OPERATION, false,
                argumentValues);
        agent1.callOperation(operationCall);

        // agent1(node1) calls an agent3(node2) operation
        var multiplyOpCall = new OperationCallWave(agent1.getID(), agent3.getID(), MULTIPLY_OPERATION, true,
                List.of(1.3d, 24.5d, 65.8d));
        agent1.callOperationWithResult(multiplyOpCall, x -> System.out.println("The result of the multiply operation is " + x));

        // agent1(node1) creates a relation with agent2(node2)
        var relation = new Relation(agent1.getID(), agent2.getID(), "agent1-agent2-relation");
        agent1.callRelationChange(Relation.RelationChangeType.CREATE, relation);

        // stop nodes
        Thread.sleep(10000);
        // node1.stop();
        // node2.stop();
    }
}
