package net.xqhs.flash.ent_op.impl;

import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.waves.RelationChangeWave;
import net.xqhs.flash.ent_op.impl.waves.ResultWave;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.InboundEntityTools;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.ResultReceiver;
import net.xqhs.flash.ent_op.model.Wave;
import net.xqhs.util.logging.Unit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.CREATE;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.REMOVE;

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

    /**
     * The map used to associate a result receiver for an operation call.
     */
    protected Map<String, ResultReceiver> resultReceiverMap;

    public DefaultEntityToolsImpl(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean initialize(EntityAPI entity) {
        operations = new HashSet<>();
        relations = new HashSet<>();
        resultReceiverMap = new HashMap<>();
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
    public boolean registerResultReceiver(String operationCallId, ResultReceiver callback) {
        if (resultReceiverMap.containsKey(operationCallId)) {
            li("The result receiver couldn't be added to the operation call with id []", operationCallId);
            return false;
        }

        resultReceiverMap.put(operationCallId, callback);
        return true;
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
        var operation = getOperation(operationName);
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
        return relations.stream()
                .filter(relation -> relation.getTo().equals(entityAPI.getEntityID()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Relation> getOutgoingRelations() {
        return relations.stream()
                .filter(relation -> relation.getFrom().equals(entityAPI.getEntityID()))
                .collect(Collectors.toSet());
    }

    @Override
    public void handleOutgoingWave(Wave wave) {
        fMas.route(wave);
    }

    @Override
    public void handleOutgoingWave(OperationCallWave operationCallWave, ResultReceiver callback) {

    }

    @Override
    public void broadcastOutgoingOperationCall(OperationCallWave operationCallWave, Set<Operation.Restriction> targets,
                                               boolean expectResults, ResultReceiver callback) {

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
                handleIncomingRelationChangeWave((RelationChangeWave) wave);
                break;
            case RESULT:
                handleIncomingResultWave((ResultWave) wave);
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
            sendResult(operationCallWave, result);
        }
    }

    private void handleIncomingRelationChangeWave(RelationChangeWave relationChangeWave) {
        var changeType = relationChangeWave.getChangeType();
        var relation = relationChangeWave.getRelation();

        if (CREATE.equals(changeType)) {
            createRelation(relation);
        } else if (REMOVE.equals(changeType)) {
            removeRelation(relation);
        }
    }

    private void handleIncomingResultWave(ResultWave resultWave) {
        var operationCallId = resultWave.getOperationCallId();
        if (!resultReceiverMap.containsKey(operationCallId)) {
            le("Couldn't find a result receiver for the operation call with id []", operationCallId);
        }
        var callback = resultReceiverMap.get(operationCallId);
        var result = resultWave.getResult();
        callback.resultNotification(result);
    }

    public boolean createRelation(Relation relation) {
        if (relations.contains(relation)) {
            li("The relation between [] and [] already exists.", relation.getFrom(), relation.getTo());
            return false;
        }

        relations.add(relation);
        lw("The relation between [] and [] has been successfully added.", relation.getFrom(), relation.getTo());
        return true;
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

    private void sendResult(OperationCallWave operationCallWave, Object result) {
        var sourceId = operationCallWave.getSourceEntity();
        var targetId = operationCallWave.getTargetEntity();
        var resultWave = new ResultWave(targetId, sourceId, operationCallWave.getId(), result);
        handleOutgoingWave(resultWave);
    }
}
