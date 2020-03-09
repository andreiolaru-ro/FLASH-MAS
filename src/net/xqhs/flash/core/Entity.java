/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * An element in the deployment, be it a support infrastructure, an agent, a shard, etc. It needs to have some sort of
 * persistent presence in the system, and therefore it has a life-cycle that can be started, stopped, and checked upon.
 * <p>
 * It may have a name that is unique in the system.
 * <p>
 * Entities can be placed one in the context of one another, but one entity can have only one type of context that
 * directly contains it (albeit it may run in the context of multiple entities of the same type).
 * <p>
 * <b>Access control policy</b>
 * <p>
 * An object that has a reference to an {@link Entity} instance is able to <b>control</b> that entity, i.e. it is able
 * to start it, stop it, and change its context.
 * <p>
 * Objects that need to call methods in an entity (such as other entities in this entity's context) should do so through
 * a <i>proxy</i>. The proxy to an entity should be obtained by calling its {@link #asContext()} method. All proxies to
 * entities should implement {@link EntityProxy}. The workflow should be as follows:
 * <ul>
 * <li>an object <i>C</i> authorized to control entities <i>E</i> and <i>S</i> has a reference to entities <i>E</i> and
 * <i>S</i>.
 * <li>object <i>C</i> calls <code>E.{@link #asContext()}</code> to obtain a proxy <i>pE</i> to entity <i>E</i>.
 * <li>object <i>C</i> calls <code>S.addContext(pE)</code> in order to integrate <i>S</i> in the context of <i>E</i>.
 * </ul>
 * <p>
 * <b>Loading</b>
 * <p>
 * Normally, before being started, {@link Entity} instances are created by {@link Loader} instances.
 * <p>
 * It is recommended that an entity receives its configuration via a {@link MultiTreeMap}. This can be done via a
 * constructor which takes a {@link MultiTreeMap} as argument, or using a default constructor and a method that takes a
 * {@link MultiTreeMap} as argument. For the latter, the {@link ConfigurableEntity} interface can be used.
 * 
 * @param <P>
 *                - the type of the entity that can contain (be the context of) this entity.
 * 
 * @author andreiolaru
 */
public interface Entity<P extends Entity<?>> extends Serializable
{
	/**
	 * Starts the life-cycle of the entity. If this goes well, from this moment on the entity should be executing
	 * normally.
	 * <p>
	 * The method must guarantee that once it has been started successfully, it can immediately begin receiving events,
	 * even if those events will not be processed immediately.
	 * 
	 * @return <code>true</code> if the entity was started without error. <code>false</code> otherwise.
	 */
	public boolean start();
	
	/**
	 * Stops the entity. After this method succeeds, the entity should not be executing any more.
	 * 
	 * @return <code>true</code> if the entity was stopped without error. <code>false</code> otherwise.
	 */
	public boolean stop();
	
	/**
	 * Queries the entity to check if it has completed its startup and is fully functional. The entity is running after
	 * it has fully {@link #start}ed and until it is {@link #stop}ed.
	 * 
	 * @return <code>true</code> if the entity is currently running.
	 */
	public boolean isRunning();
	
	/**
	 * Retrieves the name (or other identification) of the entity, if any.
	 * 
	 * @return the name, if any has been given; <code>null</code> otherwise.
	 */
	public String getName();
	
	/**
	 * Creates a link from a subordinate entity to an entity containing it in some way.
	 * <p>
	 * This method should be <b>idempotent</b> (when called with the same argument): adding the same context multiple
	 * times (with no {@link #removeContext} operations in between) should have no effect.
	 * 
	 * @param context
	 *                    - a reference to the higher-level entity.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean addContext(EntityProxy<P> context);
	
	/**
	 * Creates a link from a subordinate entity to an entity containing it in some way.
	 * <p>
	 * This method should only be used (as opposed to {@link #addContext} in the following cases:
	 * <ul>
	 * <li>when a context other than the direct parent of the entity is added; or
	 * <li>when the caller cannot know the exact type of the entity's parent; <b>and</b>
	 * <li>passing as argument an entity of the appropriate type is not possible.
	 * </ul>
	 * <p>
	 * It is the responsibility of the called object to verify the correctness of the argument.
	 * <p>
	 * The recommended implementation for this method is one that calls {@link #addContext(EntityProxy)} after casting
	 * the argument to the appropriate type, optionally catching {@link ClassCastException} and returning
	 * <code>false</code> if such an exception occurs.
	 * <p>
	 * This method should be <b>idempotent</b> (when called with the same argument): adding the same context multiple
	 * times (with no {@link #removeContext} operations in between) should have no effect.
	 * 
	 * @param context
	 *                    - a reference to the higher-level entity.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context);
	
	/**
	 * Removes the link from a subordinate entity to an entity containing it in some way.
	 * 
	 * @param context
	 *                    - a reference to the higher-level entity.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean removeContext(EntityProxy<P> context);
	
	/**
	 * Returns a <i>proxy</i> to this entity.
	 * 
	 * @param <C>
	 *                should be the actual class of this entity.
	 * @return a proxy to the entity, implementing {@link EntityProxy} parameterized with the class of this entity.
	 */
	public <C extends Entity<P>> EntityProxy<C> asContext();
	
	/**
	 * Marker interface for classes that provide a proxy to an entity which is an instance of <code>C</code>.
	 * <p>
	 * If an entity <i>E</i> is of type <code>C</code>, calling {@link Entity#asContext()} for <i>E</i> should return an
	 * instance of this class.
	 * 
	 * @author Andrei Olaru
	 *
	 * @param <C>
	 *                the class of the entity for which this is a proxy.
	 */
	interface EntityProxy<C extends Entity<?>>
	{
		/**
		 * @return the name of the proxy-ed {@link Entity}, if the entity does wish to provide it (may be
		 *         <code>null</code> or may not be identical to the actual name of the entity).
		 */
		public String getEntityName();
	}
	
	/**
	 * An index of all local entities, allowing the retrieval of a string in the form of
	 * type{@value #TYPE_NAME_SEPARATOR}name{@value #NAME_INDEX_SEPARATOR}index for any entity, just by providing a
	 * reference to the entity.
	 * <p>
	 * The index is different for different entities with the same type and name.
	 * <p>
	 * In order for the entity to have such a <i>printable</i> name, it must have been previously <i>registered</i>, so
	 * as to retain its type and compute its index. Normally, registration should be done by a loader (most likely the
	 * loader that loads that entity).
	 * <p>
	 * The name of the entity is kept as provided by {@link Entity#getName()}.
	 * 
	 * @author Andrei Olaru
	 */
	public final static class EntityIndex
	{
		/**
		 * In the printed string, the separator between the entity type and its name.
		 */
		public final static String		TYPE_NAME_SEPARATOR		= ":";
		/**
		 * In the printed string, the separator between the entity name and its index.
		 */
		public final static String		NAME_INDEX_SEPARATOR	= ".";
		/**
		 * The register of all entities.
		 */
		static Map<Entity<?>, String>	register				= new HashMap<>();
		/**
		 * The register holding the largest index for a type - name combination existing in {@link #register}.
		 */
		static Map<String, Integer>		largestIndex			= new HashMap<>();
		
		/**
		 * Registers an entry for an entity.
		 * 
		 * @param entityType
		 *                       - the type of the entity (e.g. "node").
		 * @param entity
		 *                       - the entity to register.
		 * @return the printable string for this entity.
		 */
		public static String register(String entityType, Entity<? extends Entity<?>> entity)
		{
			if(entity == null)
				return null;
			if(!register.containsKey(entity))
			{
				String id = entityType + TYPE_NAME_SEPARATOR + entity.getName();
				int index = largestIndex.containsKey(id) ? largestIndex.get(id).intValue() + 1 : 0;
				largestIndex.put(id, Integer.valueOf(index));
				register.put(entity, id + NAME_INDEX_SEPARATOR + index);
			}
			return print(entity);
		}
		
		/**
		 * Returns a printable string, as if an entity is registered, but without the entity being registered (or,
		 * indeed, instantiated). The presumed name of the entity needs to be provided.
		 * 
		 * @param entityType
		 *                       - the type of the entity (e.g. "node").
		 * @param entityName
		 *                       - the name of the entity.
		 * @return the printable string for this entity, as if it were registered.
		 */
		public static String mockPrint(String entityType, String entityName)
		{
			String id = entityType + TYPE_NAME_SEPARATOR + entityName;
			int index = largestIndex.containsKey(id) ? largestIndex.get(id).intValue() + 1 : 0;
			return id + NAME_INDEX_SEPARATOR + index;
		}
		
		/**
		 * Retrieves the printable string from the register.
		 * 
		 * @param entity
		 *                   - the entity to get the string for.
		 * @return the string.
		 */
		public static String print(Entity<? extends Entity<?>> entity)
		{
			return register.get(entity);
		}
		
		/**
		 * Retrieves an extended form of the printable string for this entity, which also identifies the local machine
		 * (making the string unique in the entire deployment).
		 * 
		 * TODO
		 * 
		 * @param entity
		 *                   - the entity to get the string for.
		 * @return the extended string.
		 */
		public static String printGlobal(Entity<? extends Entity<?>> entity)
		{
			// TODO: get PlatformUtils to provide an id for the machine
			throw new UnsupportedOperationException("not implemented.");
		}
	}
}
