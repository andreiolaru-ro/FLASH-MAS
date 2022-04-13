package net.xqhs.flash.ent_op.support;

import net.xqhs.flash.ent_op.EntityAPI;
import net.xqhs.flash.ent_op.EntityTools;
import net.xqhs.flash.ent_op.OperationCall;

public interface FMas {

    /**
     * Method used to register an entity.
     *
     * @param entityName
 *              -the entity name
     * @param entityTools
     *          -the entity tools of the entity that needs to be registered
     * @return - <code>true</code> if the entity was successfully registered; <code>false</code> otherwise.
     */
    boolean registerEntity(String entityName, EntityTools entityTools);

    /**
     * Method used to route an operation call. This method only forwards an operation call to the {@link LocalRouter}
     * which will implement the routing policy.
     *
     * @param operationCall
     *          - the operation call that needs to be routed.
     */
    void route(OperationCall operationCall);
}
