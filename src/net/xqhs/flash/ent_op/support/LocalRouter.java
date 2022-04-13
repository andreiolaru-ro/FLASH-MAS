package net.xqhs.flash.ent_op.support;

import net.xqhs.flash.ent_op.EntityAPI;
import net.xqhs.flash.ent_op.Operation;
import net.xqhs.flash.ent_op.OperationCall;

public interface LocalRouter extends EntityAPI {

    /**
     * Method used to register operations added by an entity.
     *
     * @param operation
     *          - the operation that needs to be registered.
     * @return - <code>true</code> if the operation was successfully registered; <code>false</code> otherwise.
     */
    boolean registerOperation(Operation operation);

    /**
     * Method used to unregister an operation removed by an entity.
     *
     * @param operation
     *          - the operation that needs to be unregistered.
     * @return - <code>true</code> if the operation was successfully unregistered; <code>false</code> otherwise.
     */
    boolean unregisterOperation(Operation operation);

    /**
     * Routes an operation call based on the target operation.
     *
     * @param operationCall
     *          - the operation call that must be routed.
     */
    void route(OperationCall operationCall);
}
