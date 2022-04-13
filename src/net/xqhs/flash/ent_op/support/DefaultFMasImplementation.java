package net.xqhs.flash.ent_op.support;

import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.ent_op.EntityAPI;
import net.xqhs.flash.ent_op.EntityTools;
import net.xqhs.flash.ent_op.OperationCall;
import net.xqhs.flash.ent_op.testEntity.TestEntityTools;

public class DefaultFMasImplementation implements FMas {
    /**
     * The FMas instance.
     */
    private static DefaultFMasImplementation instance;

    /**
     * The map that contains the registered entities.
     */
    private static MultiValueMap entities;

    private DefaultFMasImplementation() {
        // private constructor
    }

    public static DefaultFMasImplementation getInstance() {
        if (instance == null) {
            instance = new DefaultFMasImplementation();
            entities = new MultiValueMap();
        }
        return instance;
    }

    @Override
    public boolean registerEntity(String entityName, EntityTools entityTools) {
        if (entities.containsKey(entityName))
            return false;
        entities.addObject(entityName, entityTools);
        return true;
    }

    @Override
    public void route(OperationCall operationCall) {
        // send the opCall to the local route to route the opCall
        if (!operationCall.wasRouted()) {
            LocalRouter localRouter = DefaultLocalRouterImplementation.getInstance();
            localRouter.route(operationCall);
        } else {
            String targetEntityName = operationCall.getTargetEntity().ID;
            TestEntityTools entityTools = (TestEntityTools) entities.getObject(targetEntityName);
            entityTools.handleIncomingOperationCall(operationCall);
        }
    }


}
