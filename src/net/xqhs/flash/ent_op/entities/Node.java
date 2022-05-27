package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.DefaultLocalRouterImpl;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static net.xqhs.flash.ent_op.entities.WebSocketPylon.WEBSOCKET_PYLON_CONFIG;

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
     * The framework instance.
     */
    protected FMas fMas;

    /**
     * The local router instance.
     */
    protected DefaultLocalRouterImpl localRouter;

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

    @Override
    public boolean setup(MultiTreeMap nodeConfiguration) {
        name = nodeConfiguration.get(NODE_NAME);
        setUnitName(name);
        return true;
    }

    @Override
    public boolean start() {
        pylons.stream().findFirst().ifPresent(WebSocketPylon::start);
        localRouter.start();
        isRunning = true;
        return true;
    }

    public boolean stop() {
        pylons.stream().findFirst().ifPresent(WebSocketPylon::stop);
        isRunning = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCall operationCall) {
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

    public boolean addEntity(EntityAPI entityAPI, MultiTreeMap configuration) {
        if (entityAPI instanceof Agent) {
            // agent setup
            Agent agent = (Agent) entityAPI;
            agent.setfMas(fMas);
            agent.setup(configuration);
            agent.start();

            entities.put(agent.getName(), agent);
            return true;
        }
        if (entityAPI instanceof WebSocketPylon) {
            // pylon setup
            WebSocketPylon pylon = (WebSocketPylon) entityAPI;
            pylon.setup(configuration.getSingleTree(WEBSOCKET_PYLON_CONFIG));
            localRouter = new DefaultLocalRouterImpl(pylon);
            fMas = new DefaultFMasImpl(localRouter, pylon);
            localRouter.setfMas(fMas);
            pylon.setfMas(fMas);

            pylons.add(pylon);
            entities.put(pylon.getName(), pylon);
            return true;
        }
        return false;
    }
}
