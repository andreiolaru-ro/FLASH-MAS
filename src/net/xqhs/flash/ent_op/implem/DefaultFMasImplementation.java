package net.xqhs.flash.ent_op.implem;

import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.OperationCall;

import java.util.HashMap;
import java.util.Map;

public class DefaultFMasImplementation implements FMas {
    /**
     * The FMas instance.
     */
    private static DefaultFMasImplementation instance;

    /**
     * The map that contains the registered entities.
     */
    private static Map<String, EntityTools> entities;

    private DefaultFMasImplementation() {
        // private constructor
    }

    public static DefaultFMasImplementation getInstance() {
        if (instance == null) {
            instance = new DefaultFMasImplementation();
            entities = new HashMap<>();
        }
        return instance;
    }

    @Override
    public boolean registerEntity(String entityName, EntityTools entityTools) {
        if (entities.containsKey(entityName))
            return false;
        entities.put(entityName, entityTools);
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
            EntityTools entityTools = entities.get(targetEntityName);
            entityTools.handleIncomingOperationCall(operationCall);
        }
    }


}
