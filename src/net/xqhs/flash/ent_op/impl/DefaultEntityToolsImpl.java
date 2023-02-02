package net.xqhs.flash.ent_op.impl;

import static net.xqhs.flash.ent_op.model.Relation.RelationChangeResult.APPROVED;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeResult.REJECTED;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.CREATE;
import static net.xqhs.flash.ent_op.model.Relation.RelationChangeType.REMOVE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.waves.RelationChangeResultWave;
import net.xqhs.flash.ent_op.impl.waves.RelationChangeWave;
import net.xqhs.flash.ent_op.impl.waves.ResultWave;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.InboundEntityTools;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.ResultReceiver;
import net.xqhs.flash.ent_op.model.Wave;
import net.xqhs.util.logging.Unit;

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
	protected EntityID entityID;

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

    /**
     * The counter used to generate unique ids.
     */
    protected AtomicLong counter;

    public DefaultEntityToolsImpl(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean initialize(EntityAPI entity) {
        operations = new HashSet<>();
        relations = new HashSet<>();
        resultReceiverMap = new HashMap<>();
        counter = new AtomicLong();
        entityAPI = entity;
		entityID = entity.getID();
        entityToolsName = entityID + " " + DEFAULT_ENTITY_TOOLS_NAME;
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
            li("[] operation couldn't be added to []", operation.getName(), entityID);
            return false;
        }
        operations.add(operation);
        li("[] operation successfully added to []", operation.getName(), entityID);
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
                .filter(relation -> relation.getTo().equals(entityAPI.getID()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Relation> getOutgoingRelations() {
        return relations.stream()
                .filter(relation -> relation.getFrom().equals(entityAPI.getID()))
                .collect(Collectors.toSet());
    }

    @Override
    public void handleOutgoingWave(Wave wave) {
		if(wave.getId() == null)
			// in cases where the entity routes eaves, the wave may have been originally sent by another entity
			wave.setId(generateWaveId());
        fMas.route(wave);
    }

    @Override
    public void handleOutgoingWave(OperationCallWave wave, ResultReceiver callback) {
		if(wave.getId() == null)
			// in cases where the entity routes eaves, the wave may have been originally sent by another entity
			wave.setId(generateWaveId());
        registerResultReceiver(wave.getId(), callback);
        fMas.route(wave);
    }

    @Override
    public void changeRelation(Relation.RelationChangeType changeType, Relation relation) {
        var relationChangeWave = new RelationChangeWave(changeType, relation, entityAPI.getID(), relation.getTo());
        if (REMOVE.equals(changeType)) {
            removeRelation(relation);
        }
        handleOutgoingWave(relationChangeWave);
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
            case RELATION_CHANGE_RESULT:
                handleIncomingRelationChangeResultWave((RelationChangeResultWave) wave);
                break;
            default:
                le("The wave is not supported by FLASH-MAS.");
        }
    }

    private void handleIncomingOperationCallWave(OperationCallWave operationCallWave) {
        var operationName = operationCallWave.getTargetOperation();

        if (!entityAPI.isRunning()) {
			le("[] is not running", entityID);
            return;
        }

        if (getOperation(operationName) == null && operationCallWave.getResult() == null) {
            lw("The [] operation is not supported by the [] entity", operationName, entityID);
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
            var result = createRelation(relation) ? APPROVED : REJECTED;
            sendRelationChangeResult(relationChangeWave, result);
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

    private void handleIncomingRelationChangeResultWave(RelationChangeResultWave relationChangeResultWave) {
        var result = relationChangeResultWave.getResult();
        var relation = relationChangeResultWave.getRelation();

        if (APPROVED.equals(result)) {
            relations.add(relation);
            li("The relation between [] and [] was accepted.", relation.getFrom(), relation.getTo());
        } else {
            li("The relation between [] and [] was rejected.", relation.getFrom(), relation.getTo());
        }
    }

    private boolean createRelation(Relation relation) {
        var relationChanged = entityAPI.handleRelationChange(CREATE, relation);

        if (!relationChanged) {
            return false;
        }

        if (relations.contains(relation)) {
			lw("The relation between [] and [] already exists.", relation.getFrom(), relation.getTo());
            return false;
        }

        relations.add(relation);
		li("The relation between [] and [] has been successfully added.", relation.getFrom(), relation.getTo());
        return true;
    }

    private boolean removeRelation(Relation relation) {
        if (relations.contains(relation)) {
            relations.remove(relation);
			li("The relation between [] and [] has been successfully removed.", relation.getFrom(), relation.getTo());
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

    private void sendRelationChangeResult(RelationChangeWave relationChangeWave, Relation.RelationChangeResult result) {
        var sourceId = relationChangeWave.getSourceEntity();
        var targetId = relationChangeWave.getTargetEntity();
        var relation = relationChangeWave.getRelation();
        var resultWave = new RelationChangeResultWave(targetId, sourceId, relation, result);
        handleOutgoingWave(resultWave);
    }

    private String generateWaveId() {
        return entityID + "-" + counter.incrementAndGet();
    }
}
