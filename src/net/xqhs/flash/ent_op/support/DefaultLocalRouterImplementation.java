package net.xqhs.flash.ent_op.support;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.ent_op.EntityID;
import net.xqhs.flash.ent_op.Operation;
import net.xqhs.flash.ent_op.OperationCall;
import net.xqhs.flash.ent_op.Relation;
import net.xqhs.util.logging.Unit;

public class DefaultLocalRouterImplementation extends Unit implements LocalRouter {
    /**
     * The instance of the local router
     */
    private static LocalRouter instance;

    /**
     * The multiValueMap contains the list of available operations.
     * (key, value) -> (entityName, operations supported by that entity)
     */
    private static MultiValueMap operations;

    /**
     * The corresponding entity tools for this instance.
     */
    private DefaultLocalRouterEntityTools defaultLocalRouterEntityTools;

    private DefaultLocalRouterImplementation() {
        // private constructor
    }

    public static LocalRouter getInstance() {
        if (instance == null) {
            instance = new DefaultLocalRouterImplementation();
            operations = new MultiValueMap();
        }
        return instance;
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
    public Object handleOperationCall(OperationCall operationCall) {
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public boolean registerOperation(Operation operation) {
        if (operationExists(operation))
            return false;
        operations.add(operation.getOwner(), operation.getName());
        return true;
    }

    @Override
    public boolean unregisterOperation(Operation operation) {
        if (!operationExists(operation))
            return false;
        operations.remove(operation.getOwner(), operation.getName());
        return true;
    }

    @Override
    public void route(OperationCall operationCall) {
        // internal routing
        String targetEntity = getTargetEntity(operationCall.getOperationName());
        if (targetEntity != null) {
            operationCall.setTargetEntity(new EntityID(targetEntity));
            operationCall.setRouted(true);
            DefaultFMasImplementation.getInstance().route(operationCall);
        } else { //external routing
            // TODO: the external routing of the operation calls
        }
    }

    private boolean operationExists(Operation operation) {
        return operations.getValues(operation.getOwner()).contains(operation.getName());
    }

    private boolean operationExistsOnNode(String operationName) {
        for (String key : operations.getKeys()) {
            if (operations.getValues(key).contains(operationName))
                return true;
        }
        return false;
    }

    private String getTargetEntity(String operationName) {
        for (String key : operations.getKeys()) {
            if (operations.getValues(key).contains(operationName))
                return key;
        }
        return null;
    }
}
