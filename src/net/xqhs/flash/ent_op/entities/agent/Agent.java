package net.xqhs.flash.ent_op.entities.agent;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.operations.ReceiveOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.ResultReceiver;
import net.xqhs.util.logging.Unit;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.ReceiveOperation.RECEIVE_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.CREATE;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.REMOVE;

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
     * The agent's operations.
     */
    protected List<Operation> operations;

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
        var receiveOp = new ReceiveOperation();
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
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        if (operationCall.getTargetOperation().equals(RECEIVE_OPERATION_NAME)) {
            var message = operationCall.getArgumentValues().get(0).toString();
            var sender = operationCall.getSourceEntity().ID;
            li("############ received message: [] from []", message, sender);
        }
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        if (CREATE.equals(changeType)) {
            return entityTools.createRelation(relation);
        } else if (REMOVE.equals(changeType)) {
            return entityTools.removeRelation(relation);
        }

        lw("The [] changeType is not supported by the [] entity.", changeType, agentName);
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

    public void callOperation(OperationCallWave operationCall) {
        entityTools.handleOutgoingWave(operationCall);
    }

    public void callOperationWithResult(OperationCallWave operationCall, ResultReceiver callBack) {
        entityTools.registerResultReceiver(operationCall.getId(), callBack);
        entityTools.handleOutgoingWave(operationCall);
    }

    public void setfMas(FMas fMas) {
        this.fMas = fMas;
    }
}
