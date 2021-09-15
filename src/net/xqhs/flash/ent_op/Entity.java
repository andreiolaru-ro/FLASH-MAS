/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.ent_op;

import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * An entity is any element in the deployment, be it a support infrastructure, an agent, a shard, etc. It needs to have
 * some sort of persistent presence in the system, and therefore it has a life-cycle that can be started, stopped, and
 * checked upon.
 * <p>
 * It must have a name that is unique to the entire system. This may be a composite with the node name or any other
 * container entities.
 * <p>
 * In the Entity-Operation model, in order to be able to inter-operate with any other elements of the framework, an
 * entity needs the have an associated {@link EntityTools} instance, which offers all the services of the framework and
 * the connection to other entities.
 * <p>
 * This interface should be visible from code that needs to <i>control</i> the execution of the entity.
 * <p>
 * <b>Loading</b>
 * <p>
 * Normally, before being started, {@link Entity} instances are created by {@link Loader} instances. Most instances
 * should be created by means of framework / {@link EntityTools}.
 * <p>
 * It is recommended that an entity receives its configuration via a {@link MultiTreeMap}. This should be done by means
 * of the {@link #setup(MultiTreeMap)} method.
 * 
 * @author andreiolaru
 */
public interface Entity {
	/**
	 * Starts the life-cycle of the entity. If this goes well, from this moment on the entity should be executing
	 * normally.
	 * <p>
	 * The method must guarantee that once it has been started successfully, the entity can immediately begin receiving
	 * events, even if those events will not be processed immediately.
	 * <p>
	 * The method should return immediately. It is not guaranteed that when the method returned, the entity has
	 * successfully started; this should checked using {@link #isRunning()}.
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
	 * Retrieves the name (or other identification) of the entity, if any. It is strongly recommended that a running
	 * entity has a name.
	 * 
	 * @return the name, if any has been given; <code>null</code> otherwise.
	 */
	public String getName();
	
	/**
	 * Prepares the entity for beginning its lifecycle.
	 * 
	 * @param configuration
	 *            - all the necessary configuration.
	 * @return <code>true</code> if setup is successful and the entity is ready to be started. <code>false</code> means
	 *         the the entity hasn't got the necessary configuration for it to run.
	 */
	public boolean setup(MultiTreeMap configuration);
	
	/**
	 * This version of {@link Entity} adds the method {@link Runnable#run()}, enabling whoever holds a reference to this
	 * entity to also control the thread on which the entity executes.
	 * 
	 * @author Andrei Olaru
	 */
	public interface RunnableEntity extends Entity, Runnable {
		/**
		 * The method will start the entity and will only returned after the entity has been {@link #stop()}ed.
		 */
		@Override
		public void run();
	}
}
