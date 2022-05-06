package net.xqhs.flash.ent_op.impl;

import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.OperationCall;

import java.util.HashMap;
import java.util.Map;

public class DefaultFMasImpl implements FMas {

    /**
     * The FMas instance.
     */
    private static DefaultFMasImpl instance;

    /**
     * The map that contains the registered entities.
     */
    protected static Map<String, EntityTools> entities;

    private DefaultFMasImpl() {
        // private constructor
    }

    public static DefaultFMasImpl getInstance() {
        if (instance == null) {
            instance = new DefaultFMasImpl();
            entities = new HashMap<>();
        }
        return instance;
    }

    @Override
    public EntityTools registerEntity(EntityAPI entity) {
        String entityName = entity.getName();
        if (entities.containsKey(entityName))
            return null;
        EntityTools entityTools = new DefaultEntityToolsImpl();
        entityTools.initialize(entity);
        entities.put(entityName, entityTools);
        return entityTools;
    }

    @Override
    public boolean entityExistsOnLocalNode(String entityName) {
        return entities.containsKey(entityName);
    }

    @Override
    public void route(OperationCall operationCall) {
        // send the opCall to the local route to route the opCall
        if (!operationCall.wasRouted()) {
            LocalRouter localRouter = DefaultLocalRouterImpl.getInstance();
            localRouter.route(operationCall);
        } else {
            String targetEntityName = operationCall.getTargetEntity().ID;
            EntityTools entityTools = entities.get(targetEntityName);
            entityTools.handleIncomingOperationCall(operationCall);
        }
    }

}
