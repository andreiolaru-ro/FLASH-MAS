package net.xqhs.flash.ent_op.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.ent_op.entities.Pylon;
import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.Wave;
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
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
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
    public void route(Wave wave) {
        var targetEntityId = wave.getTargetEntity().ID;

        if (fMas.entityExistsOnLocalNode(targetEntityId)) {
            routeInternalWave(wave);
        } else {
            routeExternalWave(wave);
        }
    }

    private void routeInternalWave(Wave wave) {
        li("The wave was successfully routed.");
        wave.setRouted(true);
        fMas.route(wave);
    }

    /**
     * Check each of the pylons if they support communication for that specific agent.
     * TODO: check for all future types of pylons
     */
    private void routeExternalWave(Wave wave) {
        var sourceEntityName = wave.getSourceEntity().ID;
        var targetEntity = wave.getTargetEntity().ID;

        // first check for websocket pylons
        var routerEntities = fMas.routerEntities();
        var routerEntity = routerEntities.stream().findFirst();

        if (routerEntity.isPresent() && routerEntity.get().canRoute(wave.getTargetEntity())) {
            wave.setRouted(true);
            if (routerEntity.get() instanceof WebSocketPylon) {
                li("The wave was successfully routed. Found a pylon to route the wave to [].", targetEntity);
                var webSocketPylon = (WebSocketPylon) routerEntity.get();
                webSocketPylon.send(sourceEntityName, targetEntity, serializeWave(wave));
            }
        } else {
            le("The wave couldn't be routed. Failed to find a pylon to route the wave to [].", targetEntity);
        }
    }

    public void setfMas(FMas fMas) {
        this.fMas = fMas;
    }

    public void addPylon(Pylon pylon) {
        pylons.add(pylon);
    }

    private String serializeWave(Wave wave) {
        try {
            return mapper.writeValueAsString(wave);
        } catch (JsonProcessingException e) {
            le("The wave couldn't be serialized.");
            return null;
        }
    }
}
