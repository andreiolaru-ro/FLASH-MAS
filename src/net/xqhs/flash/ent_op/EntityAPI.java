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

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.Relation.RelationChangeType;

/**
 * Defines all the methods that an entity should offer to an object which has a reference to the entity. Normally, an
 * {@link EntityTools} instance should access an entity via these methods.
 * 
 * @author andreiolaru
 */
public interface EntityAPI {
	/**
	 * Prepares the entity for beginning its lifecycle.
	 * 
	 * @param configuration
	 *            - all the necessary configuration.
	 * @return <code>true</code> if setup is successful and the entity is ready to be started. <code>false</code> means
	 *         the the entity hasn't got the necessary configuration for it to run.
	 */
	boolean setup(MultiTreeMap configuration);

	/**
	 * Starts the life-cycle of the entity. If this goes well, from this moment on the entity should be executing
	 * normally and be available to receive operation calls.
	 * <p>
	 * The method must guarantee that once it has been started successfully, the entity can immediately begin receiving
	 * events, even if those events will not be processed immediately.
	 * <p>
	 * The method should return immediately. It is not guaranteed that when the method returned, the entity has
	 * successfully started; this should checked using {@link #isRunning()}.
	 * 
	 * @return <code>true</code> if the entity was started without error. <code>false</code> otherwise.
	 */
	boolean start();
	
	/**
	 * Queries the entity to check if it has completed its startup and is fully functional. The entity is running after
	 * it has fully {@link #start}ed, but it may have stopped at some point as a result of an operation call.
	 * 
	 * @return <code>true</code> if the entity is currently running.
	 */
	boolean isRunning();
	
	/**
	 * Call an operation of the entity.
	 * 
	 * @param operationCall
	 * @return the result of the operation call, if any.
	 */
	Object handleOperationCall(OperationCall operationCall);
	
	/**
	 * The method is called when it is wished that changes are performed in the relations between this entity and other
	 * entities.
	 * 
	 * @param changeType
	 *            - whether the relation should be added or removed.
	 * @param relation
	 *            - the relation to add or remove.
	 * @return <code>true</code> if the change is accepted, <code>false</code> otherwise.
	 */
	boolean handleRelationChange(RelationChangeType changeType, Relation relation);
	
}
