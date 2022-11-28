package net.xqhs.flash.ent_op.model;

import java.util.Set;

public interface InboundEntityTools {

    /**
     * This method handles a wave that was already routed.
     *
     * @param wave
     */
    void handleIncomingWave(Wave wave);

    /**
     * This can be called either from the entity (for verification) or from the framework, in order to obtain the list
     * of operations for the associated entity.
     *
     * @return the available operations
     */
    Set<Operation> getOperationList();

    /**
     * This can be called either from the entity (for verification) or from the framework, in order to obtain the list
     * of operations for the associated entity.
     *
     * @param operationName
     * @return the operation
     */
    Operation getOperation(String operationName);

    /**
     * This method is used to register a result receiver for an operation
     *
     * @param operationCallId
     * @param resultReceiver
     * @return <code>true</code> if the result receiver was successfully registered.
     */
    boolean registerResultReceiver(String operationCallId, ResultReceiver resultReceiver);
}
