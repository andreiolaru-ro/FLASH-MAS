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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xqhs.util.config.Config;

/**
 * The class acts as a collection of key-value pairs that allows multiple values for the same key (also called name).
 * <p>
 * For convenience and readability, {@link String} values are special and are added and retrieved using separate methods
 * than for {@link Object} values (we call Object values those values which are of any other type than String). A name
 * can have both String and Object values associated, but some methods may fail if values are not accessed correctly.
 * <p>
 * <code>null</code> values are allowed, but <code>get</code> methods also return <code>null</code> when names are not
 * found.
 * <p>
 * It is implemented as a map with {@link String} keys and with values that are a {@link List} of Objects. Using a map
 * improves finding entries. Using a list instead of a set ensures that entries with the same key stay in the same order
 * as added, which may be an advantage.
 * <p>
 * Historically, this functionality was achieved as a set of String-Object entries, but that was not as efficient.
 * Functionality is the same, except that the order of values for the same key is maintained.
 * <p>
 * The class is backed by a {@link LinkedHashMap} with {@link String} keys and {@link LinkedList}s of {@link Object}s.
 * <p>
 * The class extends {@link Config} and can be locked so that no changes are made thereon. However, the
 * {@link net.xqhs.util.config.Config.ConfigLockedException} thrown by locked methods is converted to a
 * {@link RuntimeException} instance.
 * <p>
 * This implementation is not synchronized. Any methods returning a collection of values/objects return collections that
 * are not backed up by the multi-map.
 * 
 * @author Andrei Olaru
 */
public class MultiValueMap extends Config implements Serializable {
	/**
	 * The class UID.
	 */
	private static final long serialVersionUID = -8204648145896154271L;
	
	/**
	 * A map simulating a set of entries String &rarr; Object.
	 */
	protected final Map<String, List<Object>> backingMap = new LinkedHashMap<>();
	
	/**
	 * Internal method that actually performs insertion.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @param insertFirst
	 *            - <code>true</code> if this new value should be inserted at the head of the list; <code>false</code>
	 *            for the tail of the list.
	 * @return the instance itself.
	 */
	protected MultiValueMap addItem(String name, Object value, boolean insertFirst) {
		locked();
		if(!backingMap.containsKey(name))
			backingMap.put(name, new ArrayList<>());
		List<Object> list = backingMap.get(name);
		if(insertFirst)
			list.add(0, value);
		else
			list.add(value);
		return this;
	}
	
	/**
	 * Adds a new parameter entry.
	 * <p>
	 * Throws an exception if the collection has been previously {@link #locked()}.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public MultiValueMap add(String name, String value) {
		return addObject(name, value);
	}
	
	/**
	 * Adds a new parameter entry.
	 * <p>
	 * Throws an exception if the collection has been previously {@link #locked()}.
	 * <p>
	 * The value is added as the first value associated with the name.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public MultiValueMap addFirst(String name, String value) {
		return addFirstObject(name, value);
	}
	
	/**
	 * Adds multiple entries for the same parameter.
	 * <p>
	 * Throws an exception if the collection has been previously {@link #locked()}.
	 * 
	 * @param name
	 *            - the name (key) of the entries.
	 * @param values
	 *            - the values to be associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public MultiValueMap addAll(String name, List<String> values) {
		for(String v : values)
			add(name, v);
		return this;
	}
	
	/**
	 * Adds a new parameter entry. This version of the method supports any {@link Object} instance as value.
	 * <p>
	 * Throws an exception if the collection has been previously {@link #locked()}.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public MultiValueMap addObject(String name, Object value) {
		return addItem(name, value, false);
	}
	
	/**
	 * Adds a new parameter entry. This version of the method supports any {@link Object} instance as value.
	 * <p>
	 * The object is added as the first value associated with the name.
	 * <p>
	 * Throws an exception if the collection has been previously {@link #locked()}.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 */
	public MultiValueMap addFirstObject(String name, Object value) {
		return addItem(name, value, true);
	}
	
	/**
	 * Retrieves all keys in the collection.
	 * 
	 * @return the key set.
	 */
	public Set<String> getKeys() {
		return backingMap.keySet();
	}
	
	/**
	 * Retrieves the first value matching the given name. It is not guaranteed that other entries with the same name do
	 * not exist. If the first found value is not a {@link String}, an exception will be thrown.
	 * <p>
	 * The {@link #getValue(String)} method is an alias of this method.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return the value of an entry with the given name, or <code>null</code> if the name is not found.
	 * @throws IllegalStateException
	 *             if the value is not a String.
	 */
	public String get(String name) {
		return get(name, null);
	}
	
	/**
	 * Same as {@link #get(String)}, but with a default value to return if the name is not found.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @param defaultValue
	 *            - the value to return if the name is not found.
	 * @return the value of an entry with the given name, or the default value if the name is not found.
	 * @throws IllegalStateException
	 *             if the value is not a String.
	 */
	public String get(String name, String defaultValue) {
		Object value = getObject(name);
		if(value == null)
			return null;
		if(value instanceof String)
			return (String) value;
		throw new IllegalStateException("Value for key [" + name + "] cannot be converted to String");
	}
	
	/**
	 * Alias for the {@link #get} method.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return the value of the entry with the given name, <code>null</code> if the name is not found.
	 * @throws IllegalStateException
	 *             if the value is not a String.
	 */
	public String getValue(String name) {
		return get(name);
	}
	
	/**
	 * Alias for the {@link #get} method.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @param defaultValue
	 *            - the value to return if the name is not found.
	 * @return the value of the entry with the given name, or the default value if the name is not found.
	 * @throws IllegalStateException
	 *             if the value is not a String.
	 */
	public String getValue(String name, String defaultValue) {
		return get(name, defaultValue);
	}
	
	/**
	 * Retrieves all values matching the given name, as a {@link List} of {@link String}. If any value is not a
	 * {@link String}, an exception will be thrown.
	 * 
	 * @param name
	 *            - the name to search for.
	 * @return a {@link List} of values associated with the name. The list is empty if no values exist.
	 */
	public List<String> getValues(String name) {
		List<String> ret = new ArrayList<>();
		for(Object value : getObjects(name))
			if(value == null)
				ret.add(null);
			else if(value instanceof String)
				ret.add((String) value);
			else
				throw new IllegalStateException("Value for key [" + name + "] cannot be converted to String");
		return ret;
	}
	
	/**
	 * Retrieves the (first) value associated with a name, as an {@link Object} instance.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return the value associated with the name, or <code>null</code> if the name is not found.
	 */
	public Object getObject(String name) {
		return getObject(name, null);
	}
	
	/**
	 * Same as {@link #getObject(String)}, but with a default value to return if the name is not found.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @param defaultObject
	 *            - the value to return if the name is not found.
	 * @return the value of the entry with the given name, or the default value if the name is not found.
	 */
	public Object getObject(String name, Object defaultObject) {
		if(!backingMap.containsKey(name))
			return null;
		return backingMap.get(name).get(0);
		
	}
	
	/**
	 * Retrieves all objects matching the given name, as a new {@link List}.
	 * 
	 * @param name
	 *            - the name to search for.
	 * @return a {@link List} of objects associated with the name. The list is empty if no values exist.
	 */
	public List<Object> getObjects(String name) {
		if(!backingMap.containsKey(name))
			return new ArrayList<>();
		return new ArrayList<>(backingMap.get(name));
	}
	
	/**
	 * Indicates whether an entry with the specified name exists.
	 * <p>
	 * Same with {@link #containsKey(String)}.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return - <code>true</code> if an entry with the specified name exists.
	 */
	public boolean isSet(String name) {
		return backingMap.containsKey(name);
	}
	
	/**
	 * Indicates whether an entry with the specified name exists.
	 * <p>
	 * Same with {@link #isSet(String)}.
	 * 
	 * @param name
	 *            - the name of the searched entry.
	 * @return - <code>true</code> if an entry with the specified name exists.
	 */
	public boolean containsKey(String name) {
		return backingMap.containsKey(name);
	}
	
	/**
	 * Removes all associations with a name and removes the name (key) from the map.
	 * <p>
	 * Throws an exception if the name does not exist.
	 * 
	 * @param name
	 *            - the name (key) to remove.
	 * @return the map itself.
	 */
	public MultiValueMap removeKey(String name) {
		if(!backingMap.containsKey(name))
			throw new IllegalArgumentException("Key [" + name + "] does not exist in the map.");
		backingMap.remove(name);
		return this;
	}
	
	/**
	 * Removes the first value associated with a name. If there are no more values associated with the name, the name is
	 * removed.
	 * <p>
	 * Throws an exception if the name does not exist.
	 * 
	 * @param name
	 *            - the name (key) to remove the first value from.
	 * @return the map itself.
	 */
	public MultiValueMap removeFirst(String name) {
		if(!backingMap.containsKey(name))
			throw new IllegalArgumentException("Key [" + name + "] does not exist in the map.");
		backingMap.get(name).remove(0);
		if(backingMap.get(name).isEmpty())
			removeKey(name);
		return this;
	}
	
	/**
	 * Removes a value associated with a name. If there are no more values associated with the name, the name is
	 * removed.
	 * <p>
	 * Throws an exception if the name does not exist or if the value is not associated with the name.
	 * <p>
	 * The {@link Object#equals} method must be correctly implemented for the given value.
	 * 
	 * @param name
	 *            - the name (key) to remove the value from.
	 * @param value
	 *            - the value to remove.
	 * @return the map itself.
	 */
	public MultiValueMap remove(String name, Object value) {
		if(!backingMap.containsKey(name))
			throw new IllegalArgumentException("Key [" + name + "] does not exist in the map.");
		if(!backingMap.get(name).contains(value))
			throw new IllegalArgumentException(
					"Value [" + value.toString() + "] is not associate with the name [" + name + "].");
		if(backingMap.get(name).size() == 1)
			removeKey(name);
		else
			backingMap.get(name).remove(value);
		return this;
	}
	
	@Override
	public String toString() {
		return backingMap.toString();
	}
	
	/**
	 * Wrapper of {@link Config#locked()} that converts the {@link net.xqhs.util.config.Config.ConfigLockedException}
	 * into an {@link IllegalStateException}.
	 * 
	 * @throws RuntimeException
	 *             if the set has been {@link #lock()}-ed.
	 */
	@Override
	public void locked() throws RuntimeException {
		try {
			super.locked();
		} catch(ConfigLockedException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	
	/**
	 * Creates a {@link String} which results from the serialization of the {@link MultiValueMap} instance.
	 * 
	 * @return the serialized string.
	 */
	public String toSerializedString() {
		return PlatformUtils.serializeObject(this);
	}
	
	/**
	 * Attempts to de-serialize a {@link MultiValueMap} instance from a {@link String}.
	 * 
	 * @param serialization
	 *            - the serialized form.
	 * @return the {@link MultiValueMap} instance extracted from the string.
	 * @throws ClassNotFoundException
	 *             when the serialization fails.
	 */
	public static MultiValueMap fromSerializedString(String serialization) throws ClassNotFoundException {
		byte[] data = Base64.getDecoder().decode(serialization);
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
			MultiValueMap o = (MultiValueMap) ois.readObject();
			ois.close();
			return o;
		} catch(IOException e) {
			throw new RuntimeException("Deserialization failed", e);
		}
	}
}
