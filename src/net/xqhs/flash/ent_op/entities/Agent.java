package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.ReceiveOperation.RECEIVE_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class Agent extends Unit implements EntityAPI {

    /**
     * Indicates whether the implementation is currently running.
     */
    protected boolean isRunning;

    /**
     * The name of the agent.
     */
    protected String agentName;

    /**
     * The id of this instance.
     */
    protected EntityID entityID;

    /**
     * The corresponding entity tools for this instance.
     */
    protected EntityTools entityTools;

    /**
     * The receive operation .
     */
    protected Operation receiveOp;

    @Override
    public boolean setup(MultiTreeMap agentConfiguration) {
        if (agentConfiguration == null)
            return false;

        agentName = agentConfiguration.getAValue(NAME_ATTRIBUTE_NAME);
        entityID = new EntityID(agentConfiguration.getAValue(ENTITY_ID_ATTRIBUTE_NAME));
        receiveOp = new ReceiveOperation();
        entityTools = DefaultFMasImpl.getInstance().registerEntity(this);
        entityTools.createOperation(receiveOp);
        setUnitName(agentName);

        return true;
    }

    @Override
    public boolean start() {
        isRunning = true;
        li("Agent [] started", agentName);
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public boolean stop() {
        isRunning = false;
        li("Agent [] stopped", agentName);
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCall operationCall) {
        if (operationCall.getOperationName().equals(RECEIVE_OPERATION_NAME)) {
            String message = operationCall.getArgumentValues().get(0).toString();
            String sender = operationCall.getSourceEntity().ID;
            li("received message: [] from []", message, sender);
        }
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return agentName;
    }

    public EntityID getEntityID() {
        return entityID;
    }

    public void callOperation(OperationCall operationCall) {
        entityTools.handleOutgoingOperationCall(operationCall);
    }

    public EntityTools getEntityTools() {
        return entityTools;
    }
}
