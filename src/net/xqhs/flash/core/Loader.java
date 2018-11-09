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
 * A loader instance has the capability of creating new {@link Entity} instances.
 * <p>
 * Entities are created based on a configuration that is loaded at boot time and is represented as a
 * {@link TreeParameterSet}.
 * 
 * @param <T>
 *            the type of {@link Entity} instance that the loader can load.
 * 
 * @author andreiolaru
 */
public interface Loader<T extends Entity<?>>
{
	/**
	 * Performs checks and completes the configuration.
	 * <p>
	 * This method <i>may</i> be implemented by implementing classes in order to
	 * <ul>
	 * <li>Check that an {@link Entity} of type <code>T</code> and with the given configuration can indeed be loaded; it
	 * this method returns <code>true</code>, then calling {@link #load(TreeParameterSet)} with the same configuration
	 * is expected to complete successfully.
	 * <li>Add new elements to the given configuration so as to improve the subsequent performance of a call to
	 * {@link #load} with the same configuration.
	 * </ul>
	 * Depending on each specific {@link Loader} implementation, the call to {@link #preload} may be optional or
	 * mandatory, but it is recommended that {@link #load} checks if the configuration has been pre-loaded and, if not,
	 * to call {@link #preload}.
	 * 
	 * @param configuration
	 *            - the configuration of the entity that one intends to load. This COnfiguration may be modified (added
	 *            to) in this method.
	 * @return <code>true</code> if {@link #load}ing the entity is expected to complete successfully; <code>false</code>
	 *         if the entity cannot load with the given configuration.
	 */
	public boolean preload(TreeParameterSet configuration);
	
	/**
	 * Loads a new instance of entity <b>T</b>.
	 * 
	 * @param configuration
	 *            - the configuration data for the entity.
	 * @return the entity, if loading has been successful.
	 */
	public T load(TreeParameterSet configuration);
}
