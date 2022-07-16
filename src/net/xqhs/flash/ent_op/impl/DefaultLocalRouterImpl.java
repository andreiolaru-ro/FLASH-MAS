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
import java.util.Optional;
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
        this.fMas = fMas;
    }

    public DefaultLocalRouterImpl() {}

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
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return DEFAULT_LOCAL_ROUTER_NAME;
    }

    @Override
    public void route(OperationCall operationCall) {
        String targetEntityName = operationCall.getTargetEntity().ID;
        String sourceEntityName = operationCall.getSourceEntity().ID;

        Optional<Pylon> pylonAbleToRoute =  anyPylonAbleToRoute(targetEntityName);
        if ((pylonAbleToRoute).isPresent()) {
            operationCall.setRouted(true);
            li("Found pylon to route the op call.");

            if (pylonAbleToRoute.get() instanceof WebSocketPylon) {
                WebSocketPylon webSocketPylon = (WebSocketPylon) pylonAbleToRoute.get();
                webSocketPylon.send(sourceEntityName, targetEntityName, serializeOpCall(operationCall));
            }
        } else {
            lw("No pylon found to route the op call");
        }
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

    private Optional<Pylon> anyPylonAbleToRoute(String targetEntityName) {
        // check each of the pylons if they support communication for that specific agent
        return pylons.stream().findFirst();
    }

}
