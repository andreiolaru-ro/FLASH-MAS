package net.xqhs.flash.ent_op.impl;

import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.InboundEntityTools;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.ResultReceiver;
import net.xqhs.flash.ent_op.model.Wave;
import net.xqhs.util.logging.Unit;

import java.util.HashSet;
import java.util.Set;

public class DefaultEntityToolsImpl extends Unit implements EntityTools {

    /**
     * The default name for entity tools instances of this implementation.
     */
    protected static final String DEFAULT_ENTITY_TOOLS_NAME = "entity tools";

    /**
     * The name of the current entity tools instance.
     */
    protected String entityToolsName = DEFAULT_ENTITY_TOOLS_NAME;

    /**
     * The name of the corresponding entity for this instance.
     */
    protected String entityName;

    /**
     * The entityAPI.
     */
    protected EntityAPI entityAPI;

    /**
     * The framework.
     */
    protected FMas fMas;

    /**
     * The list of available operations.
     */
    protected Set<Operation> operations;

    /**
     * The list of relations with other entities.
     */
    protected Set<Relation> relations;

    public DefaultEntityToolsImpl(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean initialize(EntityAPI entity) {
        operations = new HashSet<>();
        relations = new HashSet<>();
        entityAPI = entity;
        entityName = entity.getName();
        entityToolsName = entityName + " " + DEFAULT_ENTITY_TOOLS_NAME;
        setUnitName(entityToolsName);
        return true;
    }

    @Override
    public Set<Operation> getOperationList() {
        return operations;
    }

    @Override
    public Operation getOperation(String operationName) {
        return operations.stream()
                .filter(operation -> operation.getName().equals(operationName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean registerResultReceiver(String operationCallId, ResultReceiver resultReceiver) {
        return false;
    }

    @Override
    public boolean createOperation(Operation operation) {
        // fails if the operation already exists
        if (getOperation(operation.getName()) != null) {
            li("[] operation couldn't be added to []", operation.getName(), entityName);
            return false;
        }
        operations.add(operation);
        li("[] operation successfully added to []", operation.getName(), entityName);
        return true;
    }

    @Override
    public boolean removeOperation(String operationName) {
        Operation operation = getOperation(operationName);
        if (operation == null)
            return false;
        operations.remove(operation);
        return true;
    }

    @Override
    public Set<Relation> getRelations() {
        return relations;
    }

    @Override
    public Set<Relation> getIncomingRelations() {
//        return relations.stream().filter(relation -> relation.getTo().equals(enityId));
        return null;
    }

    @Override
    public Set<Relation> getOutgoingRelations() {
//        return relations.stream().filter(relation -> relation.getFrom().equals(enityId));
        return null;
    }

    @Override
    public void handleOutgoingOperationCall(OperationCallWave operationCallWave) {
        fMas.route(operationCallWave);
    }

    @Override
    public void handleOutgoingOperationCall(OperationCallWave operationCallWave, ResultReceiver callback) {

    }

    @Override
    public void broadcastOutgoingOperationCall(OperationCallWave operationCallWave, Set<Operation.Restriction> targets, boolean expectResults, ResultReceiver callback) {

    }

    @Override
    public Object handleOperationCallBlocking(OperationCallWave operationCallWave) {
        return null;
    }

    @Override
    public boolean linkInboundEntityTools(InboundEntityTools inboundEntityTools) {
        return false;
    }

    @Override
    public void handleIncomingWave(Wave wave) {
        switch (wave.getType()) {
            case OPERATION_CALL:
                handleIncomingOperationCallWave((OperationCallWave) wave);
                break;
            case RELATION_CHANGE:
                handleIncomingRelationChangeWave(wave);
                break;
            case RESULT:
                handleIncomingResultWave(wave);
                break;
            default:
                le("The wave is not supported by FLASH-MAS.");
        }
    }

    private void handleIncomingOperationCallWave(OperationCallWave operationCallWave) {
        var operationName = operationCallWave.getTargetOperation();

        if (!entityAPI.isRunning()) {
            le("[] is not running", entityAPI.getName());
            return;
        }

        if (getOperation(operationName) == null && operationCallWave.getResult() == null) {
            lw("The [] operation is not supported by the [] entity", operationName, entityName);
            return;
        }

        var result = entityAPI.handleIncomingOperationCall(operationCallWave);

        if (operationCallWave.isSendReturnValue()) {
            var sourceId = operationCallWave.getSourceEntity();
            var targetId = operationCallWave.getTargetEntity();
            operationCallWave.setTargetEntity(sourceId);
            operationCallWave.setSourceEntity(targetId);
            operationCallWave.setResult(result);
            operationCallWave.setRouted(false);
            operationCallWave.setSendReturnValue(false);
            handleOutgoingOperationCall(operationCallWave);
        }
    }


    private void handleIncomingRelationChangeWave(Wave wave) {
    }

    private void handleIncomingResultWave(Wave wave) {
    }

    public boolean createRelation(Relation relation) {
        if (relations.contains(relation)) {
            lw("The relation between [] and [] has been successfully added.", relation.getFrom(), relation.getTo());
            relations.add(relation);
            return true;
        }

        li("The relation between [] and [] already exists.", relation.getFrom(), relation.getTo());
        return false;
    }

    public boolean removeRelation(Relation relation) {
        if (relations.contains(relation)) {
            relations.remove(relation);
            lw("The relation between [] and [] has been successfully removed.", relation.getFrom(), relation.getTo());
            return true;
        }

        li("Couldn't remove the relation between [] and []. The relation doesn't exists.", relation.getFrom(), relation.getTo());
        return false;
    }

}
