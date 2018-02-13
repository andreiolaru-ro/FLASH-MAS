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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.xqhs.util.config.Config;

/**
 * The class acts as a collection of key-value pairs that allows multiple values for the same key. Only addition and
 * query is supported.
 * <p>
 * For convenience and readability, String values are special and are added and retrieved using separate methods than
 * for Object values.
 * <p>
 * It is implemented as a map with String keys and with values that are a {@link List} of Objects. Using a map improves
 * finding entries. Using a list instead of a set ensures that entries with the same key stay in the same order as
 * added, which may be an advantage.
 * <p>
 * Historically, this functionality was achieved as a set of String-Object entries, but that was not as efficient.
 * Functionality is the same, except that the order of values for the same key is maintained.
 * <p>
 * The class extends {@link Config} and can be locked so that no changes are made thereon. However, the
 * {@link net.xqhs.util.config.Config.ConfigLockedException} thrown by locked methods is converted to a
 * {@link RuntimeException} instance.
 * 
 * @author Andrei Olaru
 */
public class ParameterSet extends Config implements Serializable
{
	/**
	 * The class UID.
	 */
	private static final long					serialVersionUID	= -8204648145896154271L;
	
	/**
	 * A map simulating a set of entries String &rarr; Object.
	 */
	protected final Map<String, List<Object>>	parameterSet		= new LinkedHashMap<>();
	
	/**
	 * Adds a new parameter entry.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public ParameterSet add(String name, String value)
	{
		return addObject(name, value);
	}
	
	/**
	 * Adds multiple entries for the same parameter.
	 * 
	 * @param name
	 *            - the name (key) of the entries.
	 * @param values
	 *            - the values to be associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public ParameterSet addAll(String name, List<String> values)
	{
		for(String v : values)
			add(name, v);
		return this;
	}
	
	/**
	 * Adds a new parameter entry. This version of the method supports any {@link Object} instance as value.
	 * <p>
	 * This is the only method in the implementation actually performing an addition.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public ParameterSet addObject(String name, Object value)
	{
		locked();
		if(!parameterSet.containsKey(name))
			parameterSet.put(name, new ArrayList<>());
		parameterSet.get(name).add(value);
		return this;
	}
	
	/**
	 * Retrieves the first value matching the given name. It is not guaranteed that other entries with the same name do
	 * not exist. If the first found value is not a {@link String}, an exception will be thrown.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return the value of an entry with the given name, or <code>null</code> if the name is not found.
	 */
	public String get(String name)
	{
		Object value = getObject(name);
		if(value == null)
			return null;
		if(value instanceof String)
			return (String) value;
		throw new IllegalStateException("Value cannot be converted to String");
	}
	
	/**
	 * Alias for the {@link #get} method.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return the value of an entry with the given name.
	 */
	public String getValue(String name)
	{
		return get(name);
	}
	
	/**
	 * Retrieves all values matching the given name, as a {@link List} of {@link String}. If any value is not a
	 * {@link String}, an exception will be thrown.
	 * 
	 * @param name
	 *            - the name to search for.
	 * @return a {@link List} of values associated with the name. The list is empty if no values exist.
	 */
	public List<String> getValues(String name)
	{
		List<String> ret = new ArrayList<>();
		for(Object value : getObjects(name))
			if(value instanceof String)
				ret.add((String) value);
			else
				throw new IllegalStateException("Value cannot be converted to String");
		return ret;
	}
	
	/**
	 * Retrieves the (first) value associated with a name, as an {@link Object} instance.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return the value associated with the name, or <code>null</code> if the name is not found.
	 */
	public Object getObject(String name)
	{
		if(!parameterSet.containsKey(name))
			return null;
		return parameterSet.get(name).get(0);
	}
	
	/**
	 * Retrieves all objects matching the given name, as a {@link List}.
	 * 
	 * @param name
	 *            - the name to search for.
	 * @return a {@link List} of objects associated with the name. The list is empty if no values exist
	 */
	public List<Object> getObjects(String name)
	{
		if(!parameterSet.containsKey(name))
			return new ArrayList<>();
		return parameterSet.get(name);
	}
	
	/**
	 * Indicates whether an entry with the specified name exists.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return - <code>true</code> if an entry with the specified name exists.
	 */
	public boolean isSet(String name)
	{
		return parameterSet.containsKey(name);
	}
	
	@Override
	public String toString()
	{
		return parameterSet.toString();
	}
	
	/**
	 * Wrapper of {@link Config#locked()} that converts the {@link net.xqhs.util.config.Config.ConfigLockedException}
	 * into an {@link IllegalStateException}.
	 * 
	 * @throws RuntimeException
	 *             if the set has been {@link #lock()}-ed.
	 */
	@Override
	public void locked() throws RuntimeException
	{
		try
		{
			super.locked();
		} catch(ConfigLockedException e)
		{
			throw new IllegalStateException(e.getMessage());
		}
	}
}
