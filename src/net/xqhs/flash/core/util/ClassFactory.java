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
package net.xqhs.flash.core.util;

import java.lang.reflect.InvocationTargetException;

/**
 * Interface for platform-specific class-loading classes.
 * 
 * @author andreiolaru
 */
public interface ClassFactory
{
	/**
	 * @param classPath
	 *            - classpath for the class to load.
	 * @param creationData
	 *            - data to use for creating the new instance. This may be <code>null</code>.
	 * @param splitArguments
	 *            - if <code>true</code>, all first values of simple keys from <code>creationData</code> are passed
	 *            individually to the constructor. If <code>splitArguments</code> is <code>true</code> and
	 *            <code>creationData</code> is <code>null</code>, the constructor will be called with no arguments.
	 * @return a new instance for the class.
	 * @throws ClassNotFoundException if the class was not found.
	 * @throws InstantiationException if the class could not be instantiated.
	 * @throws NoSuchMethodException if an adequate constructor was not found.
	 * @throws IllegalAccessException if the constructor is not accessible.
	 * @throws InvocationTargetException if the constructor could not be invoked.
	 */
	public Object loadClassInstance(String classPath, TreeParameterSet creationData, boolean splitArguments)
			throws ClassNotFoundException, InstantiationException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException;
	
	/**
	 * @param classPath
	 *            - classpath for the class to load.
	 * @return <code>true</code> if the class can be loaded using
	 *         {@link #loadClassInstance(String, TreeParameterSet, boolean)}.
	 */
	public boolean canLoadClass(String classPath);
}
