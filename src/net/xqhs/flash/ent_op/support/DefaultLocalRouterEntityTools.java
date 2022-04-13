package net.xqhs.flash.ent_op.support;

import net.xqhs.flash.ent_op.*;

import java.util.Set;

public class DefaultLocalRouterEntityTools implements EntityTools {
    @Override
    public boolean initialize(EntityAPI entity) {
        return false;
    }

    @Override
    public Set<Operation> getOperationList() {
        return null;
    }

    @Override
    public Operation getOperation(String operationName) {
        return null;
    }

    @Override
    public boolean createOperation(Operation operation) {
        return false;
    }

    @Override
    public boolean removeOperation(String operationName) {
        return false;
    }

    @Override
    public Set<Relation> getRelations() {
        return null;
    }

    @Override
    public Set<Relation> getIncomingRelations() {
        return null;
    }

    @Override
    public Set<Relation> getOutgoingRelations() {
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
}
