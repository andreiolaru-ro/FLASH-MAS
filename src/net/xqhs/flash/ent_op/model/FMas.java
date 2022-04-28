package net.xqhs.flash.ent_op.model;

public interface FMas {

    /**
     * Method used to register an entity.
     *
     * @param entity
     *          -the entity that needs to be registered
     *
     * @return - {@link EntityTools} if the entity was successfully registered; null otherwise.
     */
    EntityTools registerEntity(EntityAPI entity);

    /**
     * Method used to check if an entity is present on the local node.
     *
     * @param entityName
     *          -the entity name
     * @return <code>true</code> if the entity is present on the current node; <code>false</code> otherwise.
     */
    boolean entityExistsOnLocalNode(String entityName);

    /**
     * Method used to route an operation call. This method only forwards an operation call to the {@link LocalRouter}
     * which will implement the routing policy.
     *
     * @param operationCall
     *          - the operation call that needs to be routed.
     */
    void route(OperationCall operationCall);
}
