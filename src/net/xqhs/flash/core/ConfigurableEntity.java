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
package net.xqhs.flash.core;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.config.Configurable;

/**
 * This interface extends {@link Entity} with a method dedicated to configuring the entity, using a {@link MultiTreeMap}
 * holding a tree of multi-maps.
 * <p>
 * A {@link ConfigurableEntity} is expected to have a zero-argument constructor such that all configuration can be done
 * via {@link #configure(MultiTreeMap)}.
 * <p>
 * Since {@link ConfigurableEntity} also extends {@link Configurable}, it can be {@link #lock()}ed.
 * <p>
 * Configuration should be performed prior to adding any contexts.
 * 
 * @author Andrei Olaru
 *
 * @param <P>
 *                the type of the context of this entity (see {@link Entity}.
 */
public interface ConfigurableEntity<P extends Entity<?>> extends Entity<P>, Configurable
{
	/**
	 * Performs configuration on a newly-created instance. All configuration information should be contained in the
	 * parameter.
	 * 
	 * @param configuration
	 *            - the configuration data.
	 * @return <code>true</code> if configuration succeeded, <code>false</code> otherwise.
	 */
	public boolean configure(MultiTreeMap configuration);
}
