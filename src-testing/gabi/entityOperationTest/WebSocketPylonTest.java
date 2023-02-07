package gabi.entityOperationTest;

import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;

import java.util.List;

import static net.xqhs.flash.ent_op.impl.operations.MultiplyOperation.MULTIPLY_OPERATION;
import static net.xqhs.flash.ent_op.impl.operations.ReceiveOperation.RECEIVE_OPERATION;
import static net.xqhs.flash.ent_op.loader.Loader.AgentType.COMPUTING_AGENT;
import static net.xqhs.flash.ent_op.loader.Loader.AgentType.SIMPLE_AGENT;
import static net.xqhs.flash.ent_op.loader.Loader.createAgent;
import static net.xqhs.flash.ent_op.loader.Loader.createNode;
import static net.xqhs.flash.ent_op.loader.Loader.createPylon;
import static net.xqhs.flash.ent_op.loader.Loader.createWebSocketServer;

public class WebSocketPylonTest {

    public static void main(String[] args) throws InterruptedException {
        // start webSocketServer
        createWebSocketServer(8885);

        // ******************************************* first node setup ******************************************* //
        var fMas1 = new DefaultFMasImpl();
        var node1 = createNode(fMas1, "node1");
        var pylon1 = createPylon(fMas1, node1, "pylon1", "node1");
        var agent1 = (SimpleAgent) createAgent(fMas1, node1, pylon1, SIMPLE_AGENT, "ws://localhost:agent1");
        node1.start();

        // ******************************************* second node setup ****************************************** //
        var fMas2 = new DefaultFMasImpl();
        var node2 = createNode(fMas2, "node2");
        var pylon2 = createPylon(fMas2, node2, "pylon2", "node2");
        var pylon3 = createPylon(fMas2, node2, "pylon3", "node2");
        var agent2 = (SimpleAgent) createAgent(fMas2, node2, pylon2, SIMPLE_AGENT, "ws://localhost:agent2");
        var agent3 = (ComputingAgent) createAgent(fMas2, node2, pylon3, COMPUTING_AGENT, "ws://localhost:agent3");
        node2.start();

        // ************************************************ op call *********************************************** //
        Thread.sleep(1000);

        // agent1(node1) calls an agent2(node2) operation
        var operationCall = new OperationCallWave(agent1.getID(), agent2.getID(), RECEIVE_OPERATION, false,
                List.of("simple message"));
        agent1.callOperation(operationCall);

        // agent1(node1) calls an agent3(node2) operation
        var multiplyOpCall = new OperationCallWave(agent1.getID(), agent3.getID(), MULTIPLY_OPERATION, true,
                List.of(1.3d, 24.5d, 65.8d));
        agent1.callOperationWithResult(multiplyOpCall, x -> System.out.println("The result of the multiply operation is " + x));


        // ******************************************** relation change ******************************************* //
        Thread.sleep(1000);

        // agent1(node1) creates a relation with agent2(node2)
        var relation = new Relation(agent1.getID(), agent2.getID(), "agent1-agent2-relation");
        agent1.callRelationChange(Relation.RelationChangeType.CREATE, relation);
    }
}
