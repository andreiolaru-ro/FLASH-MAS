package net.xqhs.flash.ent_op.impl;

import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
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
    public void handleOutgoingOperationCall(OperationCall operationCall) {
        fMas.route(operationCall);
    }

    @Override
    public void handleOutgoingOperationCall(OperationCall operationCall, ResultReceiver callback) {

    }

    @Override
    public void broadcastOutgoingOperationCall(OperationCall operationCall, Set<Operation.Restriction> targets, boolean expectResults, ResultReceiver callback) {

    }

    @Override
    public Object handleOperationCallBlocking(OperationCall operationCall) {
        return null;
    }

    public void handleIncomingOperationCall(OperationCall operationCall) {
        var operationName = operationCall.getTargetOperation();

        if (!entityAPI.isRunning()) {
            le("[] is not running", entityAPI.getName());
            return;
        }

        if (getOperation(operationName) == null && operationCall.getResult() == null) {
            lw("The [] operation is not supported by the [] entity", operationName, entityName);
            return;
        }

        var result = entityAPI.handleIncomingOperationCall(operationCall);

        if (operationCall.isSendReturnValue()) {
            var sourceId = operationCall.getSourceEntity();
            var targetId = operationCall.getTargetEntity();
            operationCall.setTargetEntity(sourceId);
            operationCall.setSourceEntity(targetId);
            operationCall.setResult(result);
            operationCall.setRouted(false);
            operationCall.setSendReturnValue(false);
            handleOutgoingOperationCall(operationCall);
        }
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
