package net.xqhs.flash.ent_op.impl;

import net.xqhs.flash.ent_op.entities.Pylon;
import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.OperationCall;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
     * Added pylons used for external routing.
     */
    protected Set<Pylon> pylons = new LinkedHashSet<>();

    public DefaultFMasImpl() {
        localRouter = new DefaultLocalRouterImpl(this);
    }

    public DefaultFMasImpl(LocalRouter localRouter) {
        this.localRouter = localRouter;
    }

    @Override
    public EntityTools registerEntity(EntityAPI entity) {
        String entityName = entity.getName();
        if (entities.containsKey(entityName))
            return null;
        // EntityTools is the link between entities and FMas. there is one instance of EntityTools
        // on each entity.
        EntityTools entityTools = new DefaultEntityToolsImpl(this);
        entityTools.initialize(entity);
        // On FMas level, we map each entity with its entityTools.
        entities.put(entityName, entityTools);
        pylons.stream()
                .filter(pylon -> pylon instanceof WebSocketPylon)
                .forEach(pylon -> ((WebSocketPylon) pylon).register(entityName));
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

    public void addPylon(Pylon pylon) {
        pylons.add(pylon);
    }

}
