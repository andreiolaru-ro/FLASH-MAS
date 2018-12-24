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

import net.xqhs.flash.core.util.TreeParameterSet;

/**
 * An element in the deployment, be it a support infrastructure, an agent, a feature, etc. It needs to have some sort of
 * persistent presence in the system, and therefore it has a life-cycle that can be started, stopped, and checked upon.
 * <p>
 * It is characterized by a name or other form of identification in the system.
 * <p>
 * Entities can be placed one in the context of one another, but one entity can have only one type of context that
 * directly contains it (albeit it may run in the context of multiple entities of the same type).
 * <p>
 * Normally, before being started, {@link Entity} instances are created by {@link Loader} instances.
 * <p>
 * It is recommended that an entity receives its configuration via a {@link TreeParameterSet}. This can be done via a
 * constructor which takes a {@link TreeParameterSet} as argument, or using a default constructor and a method that
 * takes a {@link TreeParameterSet} as argument. For the latter, the {@link ConfigurableEntity} interface can be used.
 * 
 * @param <P>
 *            - the type of the entity that can contain (be the context of) this entity.
 * 
 * @author andreiolaru
 */
public interface Entity<P extends Entity<?>>
{
	/**
	 * Starts the life-cycle of the entity. If this goes well, from this moment on the entity should be executing
	 * normally.
	 * <p>
	 * The method must guarantee that once it has been started successfully, it can immediately begin to receive events,
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
	 * @return the name.
	 */
	public String getName();
	
	/**
	 * Creates a link from a subordinate entity to an entity containing it in some way.
	 * 
	 * @param context
	 *            - a reference to the higher-level entity.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean addContext(P context);
	
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
	 * The recommended implementation for this method is one that calls {@link #addContext(Entity)} after casting the
	 * argument to the appropriate type, optionally catching {@link ClassCastException} and returning <code>false</code>
	 * if such an exception occurs.
	 * 
	 * @param context
	 *            - a reference to the higher-level entity.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean addGeneralContext(Entity<?> context);
	
	/**
	 * Removes the link from a subordinate entity to an entity containing it in some way.
	 * 
	 * @param context
	 *            - a reference to the higher-level entity.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean removeContext(P context);
}
