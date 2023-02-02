package net.xqhs.flash.ent_op.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.ent_op.impl.operations.RouteOperation;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.InboundEntityTools;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.Wave;
import net.xqhs.util.logging.Unit;

public class DefaultFMasImpl extends Unit implements FMas {
	/**
	 * The map that contains the registered entities.
	 */
	protected Map<String, InboundEntityTools> entities = new HashMap<>();
	
	/**
	 * The local router.
	 */
	protected LocalRouter localRouter;
	
	/**
	 * Entities able to route.
	 */
	protected List<EntityAPI> routerEntities = new LinkedList<>();
	
	public DefaultFMasImpl() {
		localRouter = new DefaultLocalRouterImpl();
		localRouter.setup(null, this);
		setUnitName("FMAS");
	}
	
	public DefaultFMasImpl(LocalRouter router) {
		localRouter = router;
		localRouter.setup(null, this);
		setUnitName("FMAS");
	}
	
	@Override
	public boolean registerEntity(EntityAPI entity) {
		if(entity.getID() == null)
			return ler(false, "Unable to register entity with no name []", entity.toString());
		String entityName = entity.getID().ID;
		if(entities.containsKey(entityName)) {
			return ler(false, "Unable to register already existing entity []", entity.getID());
		}
		// EntityTools is the link between entities and FMas. there is one instance of EntityTools on each entity.
		EntityTools entityTools = new DefaultEntityToolsImpl(this);
		entityTools.initialize(entity);
		entity.connectTools(entityTools);
		// On FMas level, we map each entity with its entityTools.
		entities.put(entityName, entityTools);
		
		boolean isRouter = false;
		if(entityTools.getOperationList() != null
				&& entityTools.getOperationList().stream().anyMatch(o -> o instanceof RouteOperation)) {
			routerEntities.add(entity);
			isRouter = true;
		}
		li("Registered entity [] []", entity.getID(), isRouter ? "is routing entity" : "");
		return true;
	}
	
	@Override
	public boolean entityExistsOnLocalNode(String entityName) {
		return entities.containsKey(entityName);
	}
	
	@Override
	public InboundEntityTools getEntityTools(String entityName) {
		return entities.get(entityName);
	}
	
	@Override
	public List<EntityAPI> routerEntities() {
		return routerEntities;
	}
	
	@Override
	public void route(Wave wave) {
		// send the wave to the local router to be routed
		localRouter.route(wave);
	}
}
