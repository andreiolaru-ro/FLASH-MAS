package net.xqhs.flash.ent_op.test;

import net.xqhs.flash.ent_op.EntityTools;
import net.xqhs.flash.ent_op.Operation;
import net.xqhs.flash.ent_op.OperationCall;
import net.xqhs.flash.ent_op.Relation;

import java.util.HashSet;
import java.util.Set;

public class TestEntityTools implements EntityTools {
    /**
     * The default name for instances of this implementation.
     */
    protected static final String DEFAULT_ENTITY_NAME = "Default Test Entity";

    /**
     * The name of the corresponding entity for this instance.
     */
    protected String entityName = DEFAULT_ENTITY_NAME;

    /**
     * The list of available operations.
     */
    private Set<Operation> operations;

    /**
     * The list of relations with other entities.
     */
    private Set<Relation> relations;

    @Override
    public boolean initialize(String name) {
        operations = new HashSet<>();
        relations = new HashSet<>();
        if (name != null)
            entityName = name;
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
        if (getOperation(operation.getName()) != null)
            return false;
        operations.add(operation);
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

    private void checkOperation() {

    }
}
