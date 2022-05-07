package net.xqhs.flash.ent_op.impl;

import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.OperationCall;

import java.util.HashMap;
import java.util.Map;

public class DefaultFMasImpl implements FMas {
    /**
     * The map that contains the registered entities.
     */
    protected Map<String, EntityTools> entities = new HashMap<>();

    /**
     * The local router.
     */
    protected LocalRouter localRouter;

    /**
     * The pylon used for the external routing.
     */
    protected WebSocketPylon pylon;

    public DefaultFMasImpl() {
        localRouter = new DefaultLocalRouterImpl(this);
    }

    public DefaultFMasImpl(LocalRouter localRouter, WebSocketPylon pylon) {
        this.localRouter = localRouter;
        this.pylon = pylon;
    }

    @Override
    public EntityTools registerEntity(EntityAPI entity) {
        String entityName = entity.getName();
        if (entities.containsKey(entityName))
            return null;
        EntityTools entityTools = new DefaultEntityToolsImpl(this);
        entityTools.initialize(entity);
        entities.put(entityName, entityTools);
        if (pylon != null)
            pylon.register(entityName);
        return entityTools;
    }

    @Override
    public boolean entityExistsOnLocalNode(String entityName) {
        return entities.containsKey(entityName);
    }

    @Override
    public void route(OperationCall operationCall) {
        // send the opCall to the local route to route the opCall
        if (!operationCall.isRouted()) {
            localRouter.route(operationCall);
        } else {
            String targetEntityName = operationCall.getTargetEntity().ID;
            EntityTools entityTools = entities.get(targetEntityName);
            entityTools.handleIncomingOperationCall(operationCall);
        }
    }

}
