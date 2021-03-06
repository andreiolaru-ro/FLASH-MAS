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
package net.xqhs.flash.core.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Extends {@link MultiValueMap} to have two types of keys for parameters. One type is designated by instances of the type
 * <b>T</b> -- 'registered' parameters. The other type is designated by normal {@link String} keys -- 'unregistered'
 * parameters.
 * <p>
 * The method {@link #getUnregisteredParameters()} is offered to return those parameters that have not been added with
 * one of the addition methods for registered parameters. The implementation marks all of the registered keys in a
 * register when values are added, so this method uses this register to eliminate registered keys.
 * <p>
 * There are no internal differences in the implementation. Keys for registered parameters are obtaining by calling the
 * {@link #toString()} method on the T instances. The user should therefore be cautious as to not have multiple
 * registered keys with the same String output, or that generate keys that are identical to the keys of unregistered
 * parameters. No protections are in place for these cases.
 * 
 * @author Andrei Olaru
 *
 * @param <T>
 *            the type to use for keys of registered parameters.
 */
public class RegisteredParameterSet<T> extends MultiValueMap
{
	/**
	 * The class UID.
	 */
	private static final long	serialVersionUID	= -6487233926750082784L;
	
	/**
	 * The set of registered keys that was used.
	 */
	protected Set<String> registeredKeys;
	
	/**
	 * Adds a new 'registered' parameter entry.
	 * 
	 * @param name
	 *            - the name of the entry, as an instance of T.
	 * @param value
	 *            - the value of the entry.
	 * @return the instance itself, for chained calls.
	 */
	public MultiValueMap add(T name, String value)
	{
		return addObject(name, value);
	}
	
	/**
	 * Adds a new 'unregistered' parameter.
	 */
	@Override
	public MultiValueMap add(String name, String value)
	{
		return addObject(name, value);
	}
	
	/**
	 * Adds a new 'registered' parameter entry. This version of the method supports any {@link Object} instance as
	 * value.
	 * 
	 * @param name
	 *            - the name of the entry, as an T instance.
	 * @param value
	 *            - the value of the entry.
	 * @return the instance itself, for chained calls.
	 */
	public MultiValueMap addObject(T name, Object value)
	{
		if(registeredKeys == null)
			registeredKeys = new HashSet<>();
		registeredKeys.add(name.toString());
		return addObject(name.toString(), value);
	}
	
	/**
	 * Adds a new 'unregistered' parameter, with a value of any type.
	 */
	@Override
	public MultiValueMap addObject(String name, Object value)
	{
		return super.addObject(name, value);
	}
	
	/**
	 * Method to retrieve those parameters which are 'unregistered', i.e. have not been added by using an instance of T.
	 * 
	 * @return a new {@link MultiValueMap} that contains parameters associated with unregistered keys.
	 */
	public MultiValueMap getUnregisteredParameters()
	{
		MultiValueMap ret = new MultiValueMap();
		for(String key: backingMap.keySet())
			if((registeredKeys == null) || !registeredKeys.contains(key))
				ret.backingMap.put(key, getObjects(key));
		return ret;
	}
}
