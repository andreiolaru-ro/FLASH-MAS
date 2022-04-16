package net.xqhs.flash.ent_op.model;

import java.util.Set;

public interface InboundEntityTools {

    /**
     *
     * @param operationCall
     */
    void handleIncomingOperationCall(OperationCall operationCall);

    /**
     * This can be called either from the entity (for verification) or from the framework, in order to obtain the list
     * of operations for the associated entity.
     *
     * @return
     */
    Set<Operation> getOperationList();

    /**
     * This can be called either from the entity (for verification) or from the framework, in order to obtain the list
     * of operations for the associated entity.
     */
    Operation getOperation(String operationName);
}
