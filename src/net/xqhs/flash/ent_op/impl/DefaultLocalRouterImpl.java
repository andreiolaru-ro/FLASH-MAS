package net.xqhs.flash.ent_op.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

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
     * The pylon used for external routing.
     */
    protected WebSocketPylon pylon;

    /**
     * The object mapper.
     */
    protected ObjectMapper mapper = new ObjectMapper();

    //used for one-node deployment
    public DefaultLocalRouterImpl(FMas fMas) {
        this.fMas = fMas;
    }

    public DefaultLocalRouterImpl(WebSocketPylon pylon) {
        this.pylon = pylon;
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

        operationCall.setRouted(true);
        pylon.send(sourceEntityName, targetEntityName, serializeOpCall(operationCall));
    }

    public void setfMas(FMas fMas) {
        this.fMas = fMas;
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
