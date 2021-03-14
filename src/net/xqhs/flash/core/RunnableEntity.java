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

/**
 * This version of {@link Entity} adds the method {@link Runnable#run()}, enabling whoever holds a reference to this
 * entity to also control the thread on which the entity executes.
 * 
 * @param <P>
 *            - the type of the entity that can contain (be the context of) this entity.
 * 
 * @author Andrei Olaru
 */
public interface RunnableEntity<P extends Entity<?>> extends Entity<P>, Runnable
{
	/**
	 * The method will start the entity and will only returned after the entity has been {@link #stop()}ed.
	 */
	@Override
	public void run();
}
