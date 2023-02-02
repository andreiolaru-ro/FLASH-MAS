package net.xqhs.flash.ent_op.model;

import java.util.List;

public interface FMas {
	
	/**
	 * Method used to register an entity.
	 *
	 * @param entity
	 *            -the entity that needs to be registered
	 * 			
	 * @return - <code>true</code> if the entity was successfully registered; <code>false</code> otherwise.
	 */
	boolean registerEntity(EntityAPI entity);
	
	/**
	 * Method used to check if an entity is present on the local node.
	 *
	 * @param entityName
	 *            -the entity name
	 * @return <code>true</code> if the entity is present on the current node; <code>false</code> otherwise.
	 */
	boolean entityExistsOnLocalNode(String entityName);
	
	/**
	 * Returns the {@link InboundEntityTools} instance associated with a given entity.
	 * 
	 * @param entityName
	 *            - the name of the entity
	 * @return the tools to access the entity.
	 */
	InboundEntityTools getEntityTools(String entityName);
	
	/**
	 * Method used to give all entities able to route. This is called by a {@link LocalRouter} in order to help with
	 * routing.
	 *
	 * @return a list of entities.
	 */
	List<EntityAPI> routerEntities();
	
	/**
	 * Method used to route a wave. This method only forwards a wave to the {@link LocalRouter} which will implement the
	 * routing policy.
	 *
	 * @param wave
	 */
	void route(Wave wave);
	
}
