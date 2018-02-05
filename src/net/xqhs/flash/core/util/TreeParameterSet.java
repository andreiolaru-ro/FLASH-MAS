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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The class acts as a tree of key-value pairs that allows multiple values for the same key.
 * 
 * The class builds on the same principles as {@link ParameterSet} but acts rather like a hierarchical ParameterSet. It
 * and has two types of keys.
 * <ul>
 * <li>Some keys are "simple", in that they act exactly like keys with String values in a ParameterSet. THere is a name
 * and one or more String values associated with the name.
 * <li>Other keys are "hierarchical" (or tree-keys). Each tree key has:
 * <ul>
 * <li>a type (of type String).
 * <li>a name (of type String). There can be only one value for each type-name pair.
 * <li>a value, which is a {@link TreeParameterSet} instance.
 * </ul>
 * </ul>
 * 
 * The class extends ParameterSet such that only the two types of values are admitted -- String and a mapping of String
 * to {@link TreeParameterSet}.
 * 
 * There is no intersection allowed between the set of names for simple values and the set of types for tree values.
 * 
 * @author andreiolaru
 */
public class TreeParameterSet extends ParameterSet
{
	/**
	 * Type for the values in the underlying {@link ParameterSet}, in the case of tree values.
	 */
	protected class TreeValue extends AbstractMap.SimpleEntry<String, TreeParameterSet>
	{
		/**
		 * Serial UID.
		 */
		private static final long serialVersionUID = 1L;
		
		/**
		 * @param name
		 *            - the name
		 * @param tree
		 *            - the tree
		 */
		public TreeValue(String name, TreeParameterSet tree)
		{
			super(name, tree);
		}
		
		/**
		 * @param a
		 *            the entry
		 */
		public TreeValue(Entry<? extends String, ? extends TreeParameterSet> a)
		{
			super(a);
		}
	}
	
	/**
	 * The class UID
	 */
	private static final long			serialVersionUID	= 4361796924244682172L;
	
	/**
	 * Keys that have String values.
	 */
	protected Set<String>				simpleKeys			= new HashSet<>();
	/**
	 * Keys that have {@link TreeParameterSet} values.
	 */
	protected Map<String, List<String>>	treeKeys			= new HashMap<>();
	
	protected int padLen = 0;
	
	protected void addKey(String name, boolean isSimple)
	{
		if(name.length() > padLen)
			padLen = name.length();
		if(isSimple)
			simpleKeys.add(name);
	}
	
	/**
	 * Adds a simple value.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given key is already present as a hierarchical key.
	 */
	@Override
	public TreeParameterSet add(String name, String value)
	{
		if(treeKeys.containsKey(name))
			throw new IllegalArgumentException("Key " + name
					+ " is already present, as a hierarchical key. It cannot be assigned to simple (String) values");
		addKey(name, true);
		return (TreeParameterSet) super.add(name, value);
	}
	
	/**
	 * Removes all values (if any) associated with the given name and replaces them with the given value.
	 * 
	 * @param name
	 *            - the name
	 * @param value
	 *            - the new value
	 * @return this instance itself, for chained calls.
	 */
	public TreeParameterSet set(String name, String value)
	{
		if(treeKeys.containsKey(name))
			throw new IllegalArgumentException("Key " + name
					+ " is already present, as a hierarchical key. It cannot be assigned to simple (String) values");
		if(super.get(name) != null)
			parameterSet.remove(name);
		addKey(name, true);
		return (TreeParameterSet) super.add(name, value);
	}
	
	@Override
	public TreeParameterSet addObject(String name, Object value)
	{
		if(value instanceof String || value instanceof AbstractMap.SimpleEntry)
			return (TreeParameterSet) super.addObject(name, value);
		throw new UnsupportedOperationException(
				"The TreeParameterSet class does not allow adding objects of arbitrary types.");
	}
	
	/**
	 * Adds a tree value for a type-key pair.
	 * 
	 * @param type
	 *            - the type.
	 * @param name
	 *            - the key.
	 * @param tree
	 *            - the {@link TreeParameterSet} instance to assign to the key.
	 * @return this instance itself, for chained calls.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given key is already present as a simple key.
	 */
	public TreeParameterSet addTree(String type, String name, TreeParameterSet tree)
	{
		if(simpleKeys.contains(name))
			throw new IllegalArgumentException(
					"Key " + name + " is already present, as a simple key. It cannot be assigned to tree values");
		if(!treeKeys.containsKey(type))
			treeKeys.put(type, new LinkedList<String>());
		treeKeys.get(type).add(name);
		return (TreeParameterSet) super.addObject(type, new TreeValue(name, tree));
	}
	
	/**
	 * Retrieves all simple (String) values associated with the given name (key).
	 * 
	 * @throws IllegalArgumentException
	 *             if the given key is not a hierarchical key (its values are not trees).
	 */
	@Override
	public List<String> getValues(String name)
	{
		if(!simpleKeys.contains(name))
			throw new IllegalArgumentException("Key " + name + " is not a simple key.");
		return super.getValues(name);
	}
	
	/**
	 * Retrieves all tree values associated with the given name (key).
	 * 
	 * @param type
	 *            - the key.
	 * @return the trees associated with the key.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given key is not a hierarchical key (its values are not trees).
	 */
	public Map<String, TreeParameterSet> getTrees(String type)
	{
		if(!treeKeys.containsKey(type))
			throw new IllegalArgumentException("Key " + type + " is not a hierarchical key.");
		Map<String, TreeParameterSet> ret = new HashMap<>();
		for(Object t : getObjects(type))
		{
			TreeValue tt = (TreeValue) t;
			ret.put(tt.getKey(), tt.getValue());
		}
		return ret;
	}
	
	public TreeParameterSet getTree(String type, String name)
	{
		if(!treeKeys.containsKey(type) || !treeKeys.get(type).contains(name))
			throw new IllegalArgumentException("The combination " + type + ":" + name + " does not exist.");
		for(Object o : parameterSet.get(type))
		{
			TreeValue tv = (TreeValue) o;
			if(tv.getKey().equals(name))
				return tv.getValue();
		}
		return null;
	}
	
	public TreeParameterSet getDeepTree(List<Map.Entry<String, String>> path)
	{
		// TODO
		return null;
	}
	
	@Override
	public String toString()
	{
		return toString("   ");
	}
	
	protected String toString(String indent)
	{
		String ret = "";
		boolean justtree = false;
		for(String key : parameterSet.keySet())
			if(simpleKeys.contains(key))
			{
				ret += (justtree ? "" : "\n") + indent + String.format("%-" + (padLen + 4) + "s", "[" + key + "]:") + parameterSet.get(key);
				justtree = false;
			}
			else
				for(String name : treeKeys.get(key))
				{
					ret += (justtree ? "" : "\n") + indent + "[" + key + ":" + name + "]:" + getTree(key, name).toString(indent + "    ");
					justtree = true;
				}
		if(ret.length() > 0)
			ret += "\n";
		return ret;
	}
}
