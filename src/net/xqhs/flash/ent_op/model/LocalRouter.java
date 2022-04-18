package net.xqhs.flash.ent_op.model;

public interface LocalRouter extends EntityAPI {

    /**
     * Routes an operation call based on the target operation.
     *
     * @param operationCall
     *          - the operation call that must be routed.
     */
    void route(OperationCall operationCall);
}
