package gabi.entityOperationTest.scenario;

import gabi.entityOperationTest.scenario.agents.ManagementAgent;
import gabi.entityOperationTest.scenario.agents.PhoneAgent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;

import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.TEACHER;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.DOOR_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.HEATING_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.MANAGEMENT_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.PHONE_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.PROJECTOR_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.SMART_BOARD_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.createAgent;
import static net.xqhs.flash.ent_op.deploy.Deploy.createNode;
import static net.xqhs.flash.ent_op.deploy.Deploy.createPylon;
import static net.xqhs.flash.ent_op.deploy.Deploy.createWebSocketServer;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.CREATE;

public class Scenario {

    public static void main(String[] args) {
        // start webSocketServer
        createWebSocketServer(8885);

        // ******************************************* phone node setup ******************************************* //
        var phoneFMas = new DefaultFMasImpl();
        var phoneNode = createNode(phoneFMas, "phoneNode");
        var phonePylon = createPylon(phoneFMas, phoneNode, "phonePylon", "phoneNode");
        var phoneAgent = (PhoneAgent) createAgent(phoneFMas, phoneNode, phonePylon, PHONE_AGENT, "ws://localhost:phoneAgent");
        phoneNode.start();


        // *********************************** operating systems lab node setup *********************************** //
        var precis1FMas = new DefaultFMasImpl();
        var precis1Node = createNode(precis1FMas, "precis1Node");
        var precis1Pylon = createPylon(precis1FMas, precis1Node, "precis1Pylon", "precis1Node");
        var precis1ManagementAgent = (ManagementAgent) createAgent(precis1FMas, precis1Node, precis1Pylon, MANAGEMENT_AGENT, "ws://localhost:precis1ManagementAgent");
        var precis1HeatingAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, HEATING_AGENT, "ws://localhost:precis1HeatingAgent");
        var precis1DoorAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, DOOR_AGENT, "ws://localhost:precis1DoorAgent");
        var precis1SmartBoardAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, SMART_BOARD_AGENT, "ws://localhost:precis1SmartBoardAgent");
        var precis1ProjectorAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, PROJECTOR_AGENT, "ws://localhost:precis1ProjectorAgent");
        var precis1LightningAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, PROJECTOR_AGENT, "ws://localhost:precis1LightningAgent");
        precis1Node.start();

        // ************************************ cloud computing lab node setup ************************************ //
        var precis2FMas = new DefaultFMasImpl();
        var precis2Node = createNode(precis2FMas, "precis2Node");
        var precis2Pylon = createPylon(precis2FMas, precis2Node, "precis2Pylon", "precis2Node");
        var precis2HeatingAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, HEATING_AGENT, "ws://localhost:precis2HeatingAgent");
        var precis2DoorAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, DOOR_AGENT, "ws://localhost:precis2DoorAgent");
        var precis2SmartBoardAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, SMART_BOARD_AGENT, "ws://localhost:precis2SmartBoardAgent");
        var precis2ProjectorAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, PROJECTOR_AGENT, "ws://localhost:precis2ProjectorAgent");
        var precis2LightningAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, PROJECTOR_AGENT, "ws://localhost:precis2LightningAgent");
        precis2Node.start();

        // *********************************************** relations ********************************************** //
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(phoneAgent.getID(), precis1HeatingAgent.getID(), TEACHER.name()));
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(precis1ManagementAgent.getID(), precis1ProjectorAgent.getID(), "MANAGER"));

        // ************************************************ op call *********************************************** //
//        var turnHeatingOnOpCall = new OperationCallWave(phoneAgent.getID(), precis1HeatingAgent.getID(), TURN_ON_OPERATION, true, null);
//        var getTempOpCall = new OperationCallWave(phoneAgent.getID(), precis1HeatingAgent.getID(), GET_OPERATION, true, null);
//        var setTempOpCall = new OperationCallWave(phoneAgent.getID(), precis1HeatingAgent.getID(), SET_OPERATION, true, List.of(22.0));
//        phoneAgent.callOperationWithResult(turnHeatingOnOpCall, System.out::println);
//        phoneAgent.callOperationWithResult(getTempOpCall, System.out::println);
//        phoneAgent.callOperationWithResult(setTempOpCall, System.out::println);

        var turnProjectorOnOpCall = new OperationCallWave(phoneAgent.getID(), precis1ProjectorAgent.getID(), TURN_ON_OPERATION, true, null);

        phoneAgent.callOperationWithResult(turnProjectorOnOpCall, System.out::println);
    }
}
