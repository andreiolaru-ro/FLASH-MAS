package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.DefaultLocalRouterImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon.WEBSOCKET_PYLON_CONFIG;

public class Node extends Unit implements EntityAPI {

    /**
     * The attribute name for the node name.
     */
    public static final String NODE_NAME = "nodeName";

    /**
     * The name of the node.
     */
    protected String name;

    /**
     * The local router instance.
     */
    protected DefaultLocalRouterImpl localRouter = new DefaultLocalRouterImpl();

    /**
     * The framework instance.
     */
    protected DefaultFMasImpl fMas = new DefaultFMasImpl(localRouter);

    /**
     * All added entities.
     */
    protected Map<String, EntityAPI> entities = new HashMap<>();

    /**
     * Added pylons.
     */
    protected Set<WebSocketPylon> pylons = new LinkedHashSet<>();

    /**
     * Indicates whether the implementation is currently running.
     */
    protected boolean isRunning;

    protected EntityID entityID;


    public Node() {
        localRouter.setfMas(fMas);
    }

    @Override
    public boolean setup(MultiTreeMap nodeConfiguration) {
        name = nodeConfiguration.get(NODE_NAME);
        entityID = new EntityID(nodeConfiguration.getAValue(NODE_NAME));
        setUnitName(name);
        return true;
    }

    @Override
    public boolean start() {
        pylons.forEach(WebSocketPylon::start);
        localRouter.start();
        isRunning = true;
        return true;
    }

    public boolean stop() {
        pylons.forEach(WebSocketPylon::stop);
        isRunning = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCallWave) {
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Operation> getOperations() {
        return null;
    }

    @Override
    public boolean canRoute(EntityID entityID) {
        return false;
    }

    @Override
    public EntityID getEntityID() {
        return entityID;
    }

    public boolean addEntity(EntityAPI entityAPI, MultiTreeMap configuration) {
        if (entityAPI instanceof Agent) {
            // agent setup
            Agent agent = (Agent) entityAPI;
            return addAgent(agent, configuration);
        }
        if (entityAPI instanceof WebSocketPylon) {
            // pylon setup
            WebSocketPylon pylon = (WebSocketPylon) entityAPI;
            return addWebSocketPylon(pylon, configuration);
        }
        return false;
    }

    private boolean addAgent(Agent agent, MultiTreeMap configuration) {
        agent.setfMas(fMas);
        agent.setup(configuration);
        agent.start();

        // store all added agents
        entities.put(agent.getName(), agent);
        return true;
    }

    private boolean addWebSocketPylon(WebSocketPylon webSocketPylon, MultiTreeMap configuration) {
        webSocketPylon.setfMas(fMas);
        webSocketPylon.setup(configuration.getSingleTree(WEBSOCKET_PYLON_CONFIG));
        localRouter.addPylon(webSocketPylon);
        fMas.addPylon(webSocketPylon);

        // store all added pylons
        pylons.add(webSocketPylon);
        entities.put(webSocketPylon.getName(), webSocketPylon);
        return true;
    }
}
