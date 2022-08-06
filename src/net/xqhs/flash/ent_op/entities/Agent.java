package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.operations.ReceiveOperation;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.operations.ReceiveOperation.RECEIVE_OPERATION_NAME;
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

    /**
     * The framework instance.
     */
    protected FMas fMas;

    public Agent() {

    }

    public Agent(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean setup(MultiTreeMap agentConfiguration) {
        if (agentConfiguration == null)
            return false;

        agentName = agentConfiguration.getAValue(NAME_ATTRIBUTE_NAME);
        entityID = new EntityID(agentConfiguration.getAValue(ENTITY_ID_ATTRIBUTE_NAME));
        receiveOp = new ReceiveOperation();
        entityTools = fMas.registerEntity(this);
        entityTools.createOperation(receiveOp);
        setUnitName(agentName);

        return true;
    }

    @Override
    public boolean start() {
        isRunning = true;
        li("Agent started");
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public boolean stop() {
        isRunning = false;
        li("Agent stopped");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCall operationCall) {
        if (operationCall.getTargetOperation().equals(RECEIVE_OPERATION_NAME)) {
            String message = operationCall.getArgumentValues().get(0).toString();
            String sender = operationCall.getSourceEntity().ID;
            li("############ received message: [] from []", message, sender);
        }
        return null;
    }

    @Override
    public Object handleIncomingOperationCallWithResult(OperationCall operationCall) {
        if (operationCall.getTargetOperation().equals(RECEIVE_OPERATION_NAME)) {
            String message = operationCall.getArgumentValues().get(0).toString();
            String sender = operationCall.getSourceEntity().ID;
            li(">>>>>>>>>>> received message: [] from []", message, sender);
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

    @Override
    public List<Operation> getOperations() {
        return null;
    }

    @Override
    public boolean canRoute(EntityID entityID) {
        return false;
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

    public void setfMas(FMas fMas) {
        this.fMas = fMas;
    }
}
