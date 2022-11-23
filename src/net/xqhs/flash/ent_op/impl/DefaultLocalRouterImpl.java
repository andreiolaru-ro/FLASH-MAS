package net.xqhs.flash.ent_op.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.ent_op.entities.Pylon;
import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.model.*;
import net.xqhs.util.logging.Unit;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DefaultLocalRouterImpl extends Unit implements LocalRouter {

    /**
     * The default name for entity tools instances of this implementation.
     */
    protected static final String DEFAULT_LOCAL_ROUTER_NAME = "local router";

    /**
     * The multiValueMap contains the list of available operations.
     * (key, value) -> (entityName, operations supported by that entity)
     */
    protected static MultiValueMap operations = new MultiValueMap();

    /**
     * The framework instance.
     */
    protected FMas fMas;

    /**
     * Added pylons used for external routing.
     */
    protected Set<Pylon> pylons = new LinkedHashSet<>();

    /**
     * The object mapper.
     */
    protected ObjectMapper mapper = new ObjectMapper();

    //used for one-node deployment
    public DefaultLocalRouterImpl(FMas fMas) {
        setUnitName(DEFAULT_LOCAL_ROUTER_NAME);
        this.fMas = fMas;
    }

    public DefaultLocalRouterImpl() {
        setUnitName(DEFAULT_LOCAL_ROUTER_NAME);
    }

    @Override
    public boolean setup(MultiTreeMap configuration) {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCall operationCall) {
        return null;
    }

    @Override
    public Object handleIncomingOperationCallWithResult(OperationCall operationCall) {
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return DEFAULT_LOCAL_ROUTER_NAME;
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
        return null;
    }

    @Override
    public void route(OperationCall operationCall) {
        var targetEntityId = operationCall.getTargetEntity().ID;

        if (fMas.entityExistsOnLocalNode(targetEntityId)) {
            routeInternalOpCall(operationCall);
        } else {
            routeExternalOpCall(operationCall);
        }
    }

    private void routeInternalOpCall(OperationCall operationCall) {
        li("The op call was successfully routed.");
        operationCall.setRouted(true);
        fMas.route(operationCall);
    }

    /**
     * Check each of the pylons if they support communication for that specific agent.
     * TODO: check for all future types of pylons
     */
    private void routeExternalOpCall(OperationCall operationCall) {
        String sourceEntityName = operationCall.getSourceEntity().ID;
        String targetEntity = operationCall.getTargetEntity().ID;

        // first check for websocket pylons
        List<EntityAPI> routerEntities = fMas.routerEntities();

        var routerEntity = routerEntities.stream().findFirst();
        if (routerEntity.isPresent() && routerEntity.get().canRoute(operationCall.getTargetEntity())) {
            operationCall.setRouted(true);
            if (routerEntity.get() instanceof WebSocketPylon) {
                li("The op call was successfully routed. Found a pylon to route message to [].", targetEntity);
                WebSocketPylon webSocketPylon = (WebSocketPylon) routerEntity.get();
                webSocketPylon.send(sourceEntityName, targetEntity, serializeOpCall(operationCall));
            }
        }
        le("The op call couldn't be routed. Failed to find a pylon to route message to [].", targetEntity);
    }

    public void setfMas(FMas fMas) {
        this.fMas = fMas;
    }

    public void addPylon(Pylon pylon) {
        pylons.add(pylon);
    }

    private String serializeOpCall(OperationCall operationCall) {
        String json = "";
        try {
            json = mapper.writeValueAsString(operationCall);
        } catch (JsonProcessingException e) {
            le("The operation call couldn't be serialized.");
        }
        return json;
    }
}
