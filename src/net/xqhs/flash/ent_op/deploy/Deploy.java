package net.xqhs.flash.ent_op.deploy;

import gabi.entityOperationTest.ComputingAgent;
import gabi.entityOperationTest.scenario.agents.CoolingAgent;
import gabi.entityOperationTest.scenario.agents.DoorAgent;
import gabi.entityOperationTest.scenario.agents.HeatingAgent;
import gabi.entityOperationTest.scenario.agents.LightningAgent;
import gabi.entityOperationTest.scenario.agents.PhoneAgent;
import gabi.entityOperationTest.scenario.agents.ProjectorAgent;
import gabi.entityOperationTest.SimpleAgent;
import gabi.entityOperationTest.scenario.agents.SmartBoardAgent;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.entities.Node;
import net.xqhs.flash.ent_op.entities.Pylon;
import net.xqhs.flash.ent_op.impl.operations.RegisterOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon;
import net.xqhs.flash.ent_op.impl.websocket.WebSocketServerEntity;
import net.xqhs.flash.ent_op.model.FMas;

import java.util.List;

import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.NODE_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_PYLON_NAME;
import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class Deploy {

    public enum AgentType {
        COOLING_AGENT, COMPUTING_AGENT, DOOR_AGENT, HEATING_AGENT, LIGHTNING_AGENT, PHONE_AGENT, PROJECTOR_AGENT, SIMPLE_AGENT, SMART_BOARD_AGENT
    }

    public static Node createNode(FMas fMas, MultiTreeMap configuration) {
        var node = new Node();
        node.setup(configuration);
        fMas.registerEntity(node);
        return node;
    }

    public static Node createNode(FMas fMas, String nodeId) {
        var configuration = new MultiTreeMap().addFirstValue(ENTITY_ID_ATTRIBUTE_NAME, nodeId);
        return createNode(fMas, configuration);
    }

    public static Pylon createPylon(FMas fMas, Node node, MultiTreeMap configuration) {
        var pylon = new WebSocketPylon();
        pylon.setup(configuration);
        fMas.registerEntity(pylon);
        node.addEntity(pylon);
        pylon.start();
        return pylon;
    }

    public static Pylon createPylon(FMas fMas, Node node, String pylonId, String nodeId) {
        var configuration = new MultiTreeMap().addSingleValue(WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                .addSingleValue(WEBSOCKET_PYLON_NAME, pylonId)
                .addSingleValue(NODE_NAME, nodeId);
        return createPylon(fMas, node, configuration);
    }

    public static Agent createAgent(FMas fMas, Node node, Pylon pylon, AgentType agentType, MultiTreeMap configuration) {
        var agent = instantiateAgent(agentType);
        agent.setup(configuration);
        fMas.registerEntity(agent);
        fMas.route(new OperationCallWave(null, pylon.getID(), RegisterOperation.REGISTER_OPERATION, false,
                List.of(agent.getID().ID)));
        node.addEntity(agent);
        return agent;
    }

    public static Agent createAgent(FMas fMas, Node node, Pylon pylon, AgentType agentType, String agentId) {
        var configuration = new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, agentId);
        return createAgent(fMas, node, pylon, agentType, configuration);
    }

    public static WebSocketServerEntity createWebSocketServer(int serverPort) {
        var webSocketServer = new WebSocketServerEntity(serverPort);
        webSocketServer.setup(new MultiTreeMap());
        webSocketServer.start();
        return webSocketServer;
    }

    private static Agent instantiateAgent(AgentType agentType) {
        switch (agentType) {
            case COOLING_AGENT:
                return new CoolingAgent();
            case COMPUTING_AGENT:
                return new ComputingAgent();
            case DOOR_AGENT:
                return new DoorAgent();
            case HEATING_AGENT:
                return new HeatingAgent();
            case LIGHTNING_AGENT:
                return new LightningAgent();
            case PHONE_AGENT:
                return new PhoneAgent();
            case PROJECTOR_AGENT:
                return new ProjectorAgent();
            case SIMPLE_AGENT:
                return new SimpleAgent();
            case SMART_BOARD_AGENT:
                return new SmartBoardAgent();
        }
        return new Agent();
    }

}
