package net.xqhs.flash.ent_op.impl;

import net.xqhs.flash.ent_op.entities.Pylon;
import net.xqhs.flash.ent_op.entities.WebSocketPylon;
import net.xqhs.flash.ent_op.entities.operations.RouteOperation;
import net.xqhs.flash.ent_op.model.*;
import net.xqhs.util.logging.Unit;

import java.util.*;

public class DefaultFMasImpl extends Unit implements FMas {
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

    /**
     * Entities able to route.
     */
    protected List<EntityAPI> routerEntities = new LinkedList<>();

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
        // EntityTools is the link between entities and FMas. there is one instance of EntityTools on each entity.
        EntityTools entityTools = new DefaultEntityToolsImpl(this);
        entityTools.initialize(entity);
        // On FMas level, we map each entity with its entityTools.
        entities.put(entityName, entityTools);
        if (entity.getOperations() != null &&
                entity.getOperations().stream().anyMatch(o -> o instanceof RouteOperation)) {
            routerEntities.add(entity);
        }
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
    public List<EntityAPI> routerEntities() {
        return routerEntities;
    }

    @Override
    public void route(OperationCall operationCall) {
        // send the opCall to the local route to route the opCall
        if (!operationCall.isRouted()) {
            localRouter.route(operationCall);
        } else {
            var targetEntityName = operationCall.getTargetEntity().ID;
            var entityTools = entities.get(targetEntityName);
            entityTools.handleIncomingOperationCall(operationCall);
        }
    }

    public void addPylon(Pylon pylon) {
        pylons.add(pylon);
    }

}
