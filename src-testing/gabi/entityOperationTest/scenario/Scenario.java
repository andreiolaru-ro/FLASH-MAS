package gabi.entityOperationTest.scenario;

import gabi.entityOperationTest.scenario.agents.ManagementAgent;
import gabi.entityOperationTest.scenario.agents.PhoneAgent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;

import java.util.List;

import static gabi.entityOperationTest.scenario.operations.ChangeSlideOperation.CHANGE_SLIDE_OPERATION;
import static gabi.entityOperationTest.scenario.operations.CloseOperation.CLOSE_OPERATION;
import static gabi.entityOperationTest.scenario.operations.EndPresentationOperation.END_PRESENTATION_OPERATION;
import static gabi.entityOperationTest.scenario.operations.ExportOperation.EXPORT_OPERATION;
import static gabi.entityOperationTest.scenario.operations.GetOperation.GET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.OpenOperation.OPEN_OPERATION;
import static gabi.entityOperationTest.scenario.operations.SetOperation.SET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.StartPresentationOperation.START_PRESENTATION_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOffOperation.TURN_OFF_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;
import static gabi.entityOperationTest.scenario.relations.ActivityRelation.OPERATING_SYSTEMS;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.INSIDE_CLASSROOM;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.MANAGER;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.TEACHER;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.DOOR_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.HEATING_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.LIGHTING_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.MANAGEMENT_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.PHONE_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.PROJECTOR_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.AgentType.SMART_BOARD_AGENT;
import static net.xqhs.flash.ent_op.deploy.Deploy.createAgent;
import static net.xqhs.flash.ent_op.deploy.Deploy.createNode;
import static net.xqhs.flash.ent_op.deploy.Deploy.createPylon;
import static net.xqhs.flash.ent_op.deploy.Deploy.createWebSocketServer;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.CREATE;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.REMOVE;

public class Scenario {

    public static void main(String[] args) throws InterruptedException {
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
        var precis1LightingAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, LIGHTING_AGENT, "ws://localhost:precis1LightingAgent");
        var precis1SmartBoardAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, SMART_BOARD_AGENT, "ws://localhost:precis1SmartBoardAgent");
        var precis1ProjectorAgent = createAgent(precis1FMas, precis1Node, precis1Pylon, PROJECTOR_AGENT, "ws://localhost:precis1ProjectorAgent");
        precis1Node.start();


        // ************************************ cloud computing lab node setup ************************************ //
        var precis2FMas = new DefaultFMasImpl();
        var precis2Node = createNode(precis2FMas, "precis2Node");
        var precis2Pylon = createPylon(precis2FMas, precis2Node, "precis2Pylon", "precis2Node");
        var precis2ManagementAgent = (ManagementAgent) createAgent(precis2FMas, precis2Node, precis2Pylon, MANAGEMENT_AGENT, "ws://localhost:precis2ManagementAgent");
        var precis2HeatingAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, HEATING_AGENT, "ws://localhost:precis2HeatingAgent");
        var precis2DoorAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, DOOR_AGENT, "ws://localhost:precis2DoorAgent");
        var precis2LightingAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, LIGHTING_AGENT, "ws://localhost:precis2LightingAgent");
        var precis2SmartBoardAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, SMART_BOARD_AGENT, "ws://localhost:precis2SmartBoardAgent");
        var precis2ProjectorAgent = createAgent(precis2FMas, precis2Node, precis2Pylon, PROJECTOR_AGENT, "ws://localhost:precis2ProjectorAgent");
        precis2Node.start();


        // *********************************************** relations ********************************************** //
        // precis1ManagementAgent
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(precis1ManagementAgent.getID(), precis1ProjectorAgent.getID(), MANAGER.name()));
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(precis1ManagementAgent.getID(), precis1SmartBoardAgent.getID(), MANAGER.name()));
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(precis1ManagementAgent.getID(), phoneAgent.getID(), OPERATING_SYSTEMS.name()));

        // phoneAgent - precis1HeatingAgent
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(phoneAgent.getID(), precis1HeatingAgent.getID(), TEACHER.name()));
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(phoneAgent.getID(), precis1HeatingAgent.getID(), OPERATING_SYSTEMS.name()));

        // phoneAgent - precis1DoorAgent
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(phoneAgent.getID(), precis1DoorAgent.getID(), TEACHER.name()));
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(phoneAgent.getID(), precis1DoorAgent.getID(), OPERATING_SYSTEMS.name()));

        // phoneAgent - precis1LightingAgent
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(phoneAgent.getID(), precis1LightingAgent.getID(), TEACHER.name()));
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(phoneAgent.getID(), precis1LightingAgent.getID(), OPERATING_SYSTEMS.name()));

        // precis2ManagementAgent
        precis2ManagementAgent.callRelationChange(CREATE, new Relation(precis2ManagementAgent.getID(), precis2ProjectorAgent.getID(), MANAGER.name()));

        // *********************************************** op calls *********************************************** //
        // turn on the heating in precis1 classroom
        var turnHeatingOnOpCall = new OperationCallWave(phoneAgent.getID(), precis1HeatingAgent.getID(), TURN_ON_OPERATION, true, null);
        var getTempOpCall = new OperationCallWave(phoneAgent.getID(), precis1HeatingAgent.getID(), GET_OPERATION, true, null);
        var setTempOpCall = new OperationCallWave(phoneAgent.getID(), precis1HeatingAgent.getID(), SET_OPERATION, true, List.of(22.0));
        phoneAgent.callOperationWithResult(turnHeatingOnOpCall, System.out::println);
        phoneAgent.callOperationWithResult(getTempOpCall, System.out::println);
        phoneAgent.callOperationWithResult(setTempOpCall, System.out::println);

        // open the precis1 classroom door
        var openDoorOpCall = new OperationCallWave(phoneAgent.getID(), precis1DoorAgent.getID(), OPEN_OPERATION, true, null);
        phoneAgent.callOperationWithResult(openDoorOpCall, System.out::println);

        // turn on the lights and set luminosity
        var turnLightingOnOpCall = new OperationCallWave(phoneAgent.getID(), precis1LightingAgent.getID(), TURN_ON_OPERATION, true, null);
        var setLuminosityOpCall = new OperationCallWave(phoneAgent.getID(), precis1LightingAgent.getID(), SET_OPERATION, true, List.of(75));
        phoneAgent.callOperationWithResult(turnLightingOnOpCall, System.out::println);
        phoneAgent.callOperationWithResult(setLuminosityOpCall, System.out::println);

        // Andreea enters the precis1 classroom
        precis1ManagementAgent.callRelationChange(CREATE, new Relation(precis1ManagementAgent.getID(), phoneAgent.getID(), INSIDE_CLASSROOM.name()));
        Thread.sleep(1000);

        // turn on the projector and start the presentation
        var turnProjectorOnOpCall = new OperationCallWave(phoneAgent.getID(), precis1ProjectorAgent.getID(), TURN_ON_OPERATION, true, null);
        var startPresentationOpCall = new OperationCallWave(phoneAgent.getID(), precis1ProjectorAgent.getID(), START_PRESENTATION_OPERATION, true, List.of("OperatingSystemsLab5.pdf"));
        var changeSlideOpCall = new OperationCallWave(phoneAgent.getID(), precis1ProjectorAgent.getID(), CHANGE_SLIDE_OPERATION, true, List.of(5));
        phoneAgent.callOperationWithResult(turnProjectorOnOpCall, System.out::println);
        phoneAgent.callOperationWithResult(startPresentationOpCall, System.out::println);
        phoneAgent.callOperationWithResult(changeSlideOpCall, System.out::println);

        // turn on the smart board and export the screen as a PDF file
        var turnSmartBoardOnOpCall = new OperationCallWave(phoneAgent.getID(), precis1SmartBoardAgent.getID(), TURN_ON_OPERATION, true, null);
        var setSmartBoardLuminosityOpCall = new OperationCallWave(phoneAgent.getID(), precis1SmartBoardAgent.getID(), SET_OPERATION, true, List.of(80));
        var exportFileOpCall = new OperationCallWave(phoneAgent.getID(), precis1SmartBoardAgent.getID(), EXPORT_OPERATION, true, List.of("PDF"));
        phoneAgent.callOperationWithResult(turnSmartBoardOnOpCall, System.out::println);
        phoneAgent.callOperationWithResult(setSmartBoardLuminosityOpCall, System.out::println);
        phoneAgent.callOperationWithResult(exportFileOpCall, System.out::println);

        // end the presentation and turn off the projector
        var endPresentationOpCall = new OperationCallWave(phoneAgent.getID(), precis1ProjectorAgent.getID(), END_PRESENTATION_OPERATION, true, null);
        var turnProjectorOffOpCall = new OperationCallWave(phoneAgent.getID(), precis1ProjectorAgent.getID(), TURN_OFF_OPERATION, true, null);
        phoneAgent.callOperationWithResult(endPresentationOpCall, System.out::println);
        phoneAgent.callOperationWithResult(turnProjectorOffOpCall, System.out::println);

        // turn off the smart board
        var turnSmartBoardOffOpCall = new OperationCallWave(phoneAgent.getID(), precis1SmartBoardAgent.getID(), TURN_OFF_OPERATION, true, null);
        phoneAgent.callOperationWithResult(turnSmartBoardOffOpCall, System.out::println);

        // Andreea leaves the precis1 classroom
        Thread.sleep(1000);
        precis1ManagementAgent.callRelationChange(REMOVE, new Relation(precis1ManagementAgent.getID(), phoneAgent.getID(), INSIDE_CLASSROOM.name()));

        // turn off the lights
        var turnLightingOffOpCall = new OperationCallWave(phoneAgent.getID(), precis1LightingAgent.getID(), TURN_OFF_OPERATION, true, null);
        phoneAgent.callOperationWithResult(turnLightingOffOpCall, System.out::println);

        // close the door
        var closeDoorOpCall = new OperationCallWave(phoneAgent.getID(), precis1DoorAgent.getID(), CLOSE_OPERATION, true, null);
        phoneAgent.callOperationWithResult(closeDoorOpCall, System.out::println);
    }
}
