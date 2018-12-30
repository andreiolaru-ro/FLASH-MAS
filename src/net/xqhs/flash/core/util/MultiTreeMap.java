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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * The class acts as a tree of key-value pairs that allows multiple values for the same key.
 * <p>
 * The class builds on the same principles as {@link MultiValueMap} but acts rather like a hierarchical MultiValueMap.
 * It has two types of keys.
 * <ul>
 * <li>Some keys are "simple", in that they act exactly like keys with String values in a {@link MultiValueMap}. There
 * is a name and one or more {@link String} values associated with the name.
 * <li>Other keys are "hierarchical" (or tree-keys). Each tree key has a name and one or more associated
 * {@link MultiTreeMap} values.
 * </ul>
 * <p>
 * The class extends {@link MultiValueMap} such that only the two types of values are admitted -- {@link String} and
 * {@link MultiTreeMap}.
 * <p>
 * There is no intersection allowed between the set of names for simple values and the set of types for tree values.
 * <p>
 * For clearer implementation and easier debugging, there are separate methods for the keys which are supposed to only
 * hold one value (call them <i>singleton</i> keys), both for simple and for tree keys. It is possible to change the
 * singleton status of keys by calling TODO
 * 
 * @author andreiolaru
 */
public class MultiTreeMap extends MultiValueMap
{
	/**
	 * The class UID
	 */
	private static final long	serialVersionUID	= 4361796924244682172L;
	
	/**
	 * Keys that have String values.
	 */
	protected Set<String>		simpleKeys			= new HashSet<>();
	/**
	 * Keys that have {@link MultiTreeMap} values.
	 */
	protected Set<String>		treeKeys			= new HashSet<>();
	/**
	 * Keys that should only hold one value (can be simple or hierarchical).
	 */
	protected Set<String>		singletonKeys		= new HashSet<>();
	
	/**
	 * Is updated with the longest length of a key; used for pretty printing without calculating the longest key every
	 * time.
	 */
	protected int				padLen				= 0;
	
	/**
	 * Internal method for adding a key (either simple or hierarchical).
	 * 
	 * @param name
	 *            - the key.
	 * @param isSimple
	 *            - <code>true</code> if the key is simple, <code>false</code> otherwise.
	 * @param isSingleton
	 *            - <code>true</code> if the key should have only one value.
	 */
	protected void addKey(String name, boolean isSimple, boolean isSingleton)
	{
		if(name.length() > padLen)
			padLen = name.length();
		if(isSimple)
			simpleKeys.add(name);
		if(isSingleton)
			singletonKeys.add(name);
	}
	
	/**
	 * Internal method for associating a value with a name. The value can be simple or a tree, and it can be singleton
	 * or not.
	 * <p>
	 * If the name is not pre-existing, it is added accordingly.
	 * <p>
	 * Exceptions are thrown if
	 * <ul>
	 * <li>The name has been added as singleton but now the new value is added as an additional value.
	 * <li>The name has been added as a non-singleton value but the new value is added as a singleton value.
	 * <li>The name is simple, but the value is a tree; or the name is hierarchical, but the value is a string.
	 * </ul>
	 * <p>
	 * If the name is existing and singleton, the given value replaces the existing association.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value, either a {@link String} or a {@link MultiTreeMap} instance.
	 * @param isSimple
	 *            - <code>true</code> if the name is expected to be simple, <code>false</code> if it is expected to be
	 *            hierarchical.
	 * @param isSingleton
	 *            - <code>true</code> if the name is associated with a singleton value, <code>false</code> if multiple
	 *            values can be associated with the name.
	 * @return the instance itself.
	 */
	protected MultiTreeMap addItem(String name, Object value, boolean isSimple, boolean isSingleton)
	{
		if(isSimple && treeKeys.contains(name))
			throw new IllegalArgumentException("Key " + name
					+ " is already present, as a hierarchical key. It cannot be assigned to simple (String) values.");
		if(!isSimple && simpleKeys.contains(name))
			throw new IllegalArgumentException(
					"Key " + name + " is already present, as a simple key. It cannot be assigned to tree values.");
		boolean keyExists = false;
		if(treeKeys.contains(name) || simpleKeys.contains(name))
		{ // existing key
			keyExists = true;
			if(isSingleton && !singletonKeys.contains(value))
				throw new IllegalArgumentException("Key " + name
						+ " is already present, as a singleton key. Multiple values cannot be added to it.");
			if(!isSingleton && singletonKeys.contains(value))
				throw new IllegalArgumentException("Key " + name
						+ " is already present, as a multi (non-singleton) key. Methods for singleton keys cannot be used for it.");
		}
		else
			addKey(name, isSimple, isSingleton);
		if(isSingleton && keyExists)
			// replace value
			backingMap.get(name).set(0, value);
		else
			addObject(name, value);
		return this;
	}
	
	/**
	 * Associates a singleton value to a name.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the given key is already present as a hierarchical key, or as a non-singleton key.
	 */
	public MultiTreeMap addSingleValue(String name, String value)
	{
		return addItem(name, value, true, true);
	}
	
	/**
	 * Associates an additional value with a name.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the given key is already present as a hierarchical key, or as a singleton key.
	 */
	public MultiTreeMap addOneValue(String name, String value)
	{
		return addItem(name, value, true, false);
	}
	
	/**
	 * Alias of {@link #addOneValue(String, String)}, but should not be used because of its ambiguity.
	 */
	@Override
	@Deprecated
	public MultiValueMap add(String name, String value)
	{
		return addOneValue(name, value);
	}
	
	@Override
	public MultiTreeMap addAll(String name, List<String> values)
	{
		if(values.isEmpty())
			// no effect
			return this;
		String first = values.get(0);
		// test key and add first value.
		addItem(name, first, true, false);
		return (MultiTreeMap) super.addAll(name, values.subList(1, values.size()));
	}
	
	/**
	 * Associates a singleton tree value to a hierarchical key .
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param tree
	 *            - the {@link MultiTreeMap} instance to associate with the name.
	 * @return this instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the given key is already present as a simple key or as a non-singleton key.
	 */
	public MultiTreeMap addSingleTree(String name, MultiTreeMap tree)
	{
		return addItem(name, tree, false, true);
	}
	
	/**
	 * Associates an additional tree to a hierarchical key .
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param tree
	 *            - the {@link MultiTreeMap} instance to associate with the name.
	 * @return this instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the given key is already present as a simple key or as a singleton key.
	 */
	public MultiTreeMap addOneTree(String name, MultiTreeMap tree)
	{
		return addItem(name, tree, false, false);
	}
	
	/**
	 * Associates multiple trees to the same name.
	 * 
	 * @param name
	 *            - the name (key).
	 * @param trees
	 *            - the {@link MultiTreeMap} instances to associate with the name.
	 * @return this instance itself, for chained calls.
	 */
	public MultiTreeMap addTrees(String name, List<MultiTreeMap> trees)
	{
		if(trees.isEmpty())
			// no effect
			return this;
		MultiTreeMap first = trees.get(0);
		addItem(name, first, false, false);
		for(MultiTreeMap t : trees.subList(1, trees.size()))
			addOneTree(name, t);
		return this;
	}
	
	@Override
	public MultiTreeMap addObject(String name, Object value)
	{
		throw new UnsupportedOperationException(
				"The MultiTreeMap class does not allow adding objects arbitrarily.");
	}
	
	/**
	 * Replaces singleton value for the given name.
	 * 
	 * @param name
	 *            - the (key) name of the entry.
	 * @param value
	 *            - the new value.
	 * @return this instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the key does not exist, is not simple, or is not singleton.
	 */
	public MultiTreeMap replaceValue(String name, String value)
	{
		return addItem(name, value, true, true);
	}
	
	/**
	 * Removes all values associated with a name but does not remove the name from the set, so it stays in the same
	 * position as it was added.
	 * <p>
	 * If the key did not previously exist, it is not created.
	 * <p>
	 * The operation is only available for non-singleton names.
	 * 
	 * @param name
	 *            - the name to clear.
	 * @return the instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the name exists and is associated with singleton values.
	 */
	public MultiTreeMap clear(String name)
	{
		if(singletonKeys.contains(name))
			throw new IllegalArgumentException("Singleton names cannot be cleared.");
		if(backingMap.containsKey(name))
			backingMap.get(name).clear();
		return this;
	}
	
	/**
	 * @param name
	 *            - the name to search.
	 * @return <code>true</code> if the name has been added to this instance as a simple key; <code>false</code>
	 *         otherwise.
	 */
	public boolean isSimple(String name)
	{
		return simpleKeys.contains(name);
	}
	
	/**
	 * Alias of {@link #isSimple(String)}.
	 * 
	 * @param name
	 *            - the name to search.
	 * @return <code>true</code> if the name has been added to this instance as a simple key; <code>false</code>
	 *         otherwise.
	 */
	public boolean containsSimpleName(String name)
	{
		return isSimple(name);
	}
	
	/**
	 * @param name
	 *            - the name to search.
	 * @return <code>true</code> if the name has been added to this instance as a hierarchical key; <code>false</code>
	 *         otherwise.
	 */
	public boolean isHierarchical(String name)
	{
		return treeKeys.contains(name);
	}
	
	/**
	 * Alias of {@link #isHierarchical(String)}.
	 * 
	 * @param name
	 *            - the name to search.
	 * @return <code>true</code> if the name has been added to this instance as a hierarchical key; <code>false</code>
	 *         otherwise.
	 */
	public boolean containsHierarchicalName(String name)
	{
		return isHierarchical(name);
	}
	
	/**
	 * @return the list of simple keys, as a new list, not backed by this map.
	 */
	public List<String> getSimpleNames()
	{
		return new LinkedList<>(simpleKeys);
	}
	
	/**
	 * @return the list of hierarchical keys, as a new list, not backed by this map.
	 */
	public List<String> getHierarchicalNames()
	{
		return new LinkedList<>(treeKeys);
	}
	
	/**
	 * Alias for {@link #getHierarchicalNames()}.
	 * 
	 * @return the list of hierarchical keys.
	 */
	public List<String> getTreeKeys()
	{
		return getHierarchicalNames();
	}
	
	/**
	 * In this implementation, {@link #get(String)} is an alias of {@link #getValue(String)}, so it must only be used
	 * for singleton simple values.
	 */
	@Override
	public String get(String name)
	{
		return super.get(name);
	}
	
	/**
	 * In this implementation, getValue should only be used for singleton values.
	 * <p>
	 * The implementation relies on {@link MultiValueMap#getValue(String)}.
	 * 
	 * @throws IllegalArgumentException
	 *             if used for a non-singleton value.
	 */
	@Override
	public String getValue(String name)
	{
		if(containsKey(name) && !singletonKeys.contains(name))
			throw new IllegalArgumentException("Key [" + name + "] exists and is not a singleton key.");
		return super.getValue(name);
	}
	
	/**
	 * The implementation relies on {@link MultiValueMap#getValue(String)}.
	 * 
	 * @param name
	 *            - the name (key) to search.
	 * @return the first value associated with the name, if any; <code>null</code> otherwise.
	 */
	public String getFirstValue(String name)
	{
		if(containsKey(name) && singletonKeys.contains(name))
			throw new IllegalArgumentException("Key [" + name + "] exists and is a singleton key.");
		return super.getValue(name);
	}
	
	/**
	 * Retrieves all simple (String) values associated with the given name (key).
	 * 
	 * @throws IllegalArgumentException
	 *             if the given key is a hierarchical key (its values are not Strings).
	 */
	@Override
	public List<String> getValues(String name)
	{
		if(treeKeys.contains(name))
			throw new IllegalArgumentException("Key " + name + " is not a simple key.");
		return super.getValues(name);
	}
	
	/**
	 * Get the value at the end of a path in a tree. The last key must be a simple key. All other keys must be
	 * hierarchical keys.
	 * <p>
	 * The method fails fast: if one of the keys (except for the last one) is not a hierarchical key, the method returns
	 * <code>null</code>.
	 * 
	 * @param keys
	 *            - the path, consisting of keys.
	 * @return the value associated with the leaf at the end of the path, if any is found; <code>null</code> otherwise.
	 */
	public String getDeepValue(String... keys)
	{
		switch(keys.length)
		{
		case 0:
			return null;
		case 1:
			return getValue(keys[0]);
		default:
			if(!isHierarchical(keys[0]))
				return null;
			return getTree(keys[0]).getDeepValue(Arrays.copyOfRange(keys, 1, keys.length));
		}
	}
	
	/**
	 * Retrieves all tree values associated with the given name (key).
	 * 
	 * @param name
	 *            - the key.
	 * @return the trees associated with the key.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given key is a simple key (its values are not trees).
	 */
	public List<MultiTreeMap> getTrees(String name)
	{
		if(simpleKeys.contains(name))
			throw new IllegalArgumentException("Key " + name + " is not a hierarchical key.");
		List<MultiTreeMap> ret = new LinkedList<>();
		for(Object t : getObjects(name))
			ret.add((MultiTreeMap) t);
		return ret;
	}
	
	/**
	 * Retrieves the first tree associated with the given name.
	 * 
	 * @param name
	 *            - the name (key).
	 * @return the first associated tree.
	 */
	public MultiTreeMap getTree(String name)
	{
		return getTree(name, false);
	}
	
	/**
	 * Retrieves the first tree associated with the given name.
	 * <p>
	 * Optionally, if no such tree exists, creates one, adds it as a value for the given name and returns it.
	 * 
	 * @param name
	 *            - the name (key).
	 * @param create
	 *            - if <code>true</code> and no tree is associated with the name, a new tree is added for the given name
	 *            and returned. If <code>false</code>, the method is the same as {@link #getTree(String)}.
	 * @return the first associated tree.
	 */
	public MultiTreeMap getTree(String name, boolean create)
	{
		List<MultiTreeMap> trees = getTrees(name);
		if(trees.isEmpty())
		{
			if(create)
			{
				MultiTreeMap newTree = new MultiTreeMap();
				addTree(name, newTree);
				return newTree;
			}
			return null;
		}
		return trees.get(0);
	}
	
	/**
	 * Custom string output, with specific depth and potentially a shorter format.
	 * 
	 * @param depth
	 *            - maximum depth to explore. Negative values mean there is no depth limit. Exploration stops when this
	 *            arguments is 0.
	 * @param shorter
	 *            - if <code>true</code>, a shorter output format will be used with no newlines, less brackets, and less
	 *            details.
	 * @return the String rendition of this tree.
	 */
	public String toString(int depth, boolean shorter)
	{
		return toString(shorter ? "" : "   ", shorter ? "" : "      ", depth, shorter);
	}
	
	@Override
	public String toString()
	{
		return toString("   ", "      ", -1, false);
	}
	
	/**
	 * Internal method for printing, that includes indenting the tree so as to present it as part of a higher-level
	 * tree.
	 * 
	 * @param indent
	 *            - the current indent.
	 * @param baseIndent
	 *            - the indent increment.
	 * @param depth
	 *            - maximum depth to explore. Negative values mean there is no depth limit. Exploration stops when this
	 *            arguments is 0.
	 * @param shorter
	 *            - if <code>true</code>, a shorter output format will be used with no newlines, less brackets, and less
	 *            details.
	 * @return the String rendition of this tree, indented.
	 */
	protected String toString(String indent, String baseIndent, int depth, boolean shorter)
	{
		if(depth == 0)
			return shorter ? "," : "";
		String ret = shorter && indent.length() < baseIndent.length() ? ">" : "";
		boolean justtree = false;
		for(String name : parameterSet.keySet())
			if(simpleKeys.contains(name))
			{
				if(shorter)
					continue;
				ret += (justtree || shorter ? "" : "\n") + indent
						+ String.format("%-" + (padLen + 4) + "s", "[" + name + "]:") + parameterSet.get(name);
				justtree = false;
			}
			else
			{
				ret += (justtree || shorter ? "" : "\n") + indent + (shorter ? "" : "[") + name + (shorter ? "" : "]>");
				justtree = true;
				boolean first = true;
				for(Object o : parameterSet.get(name))
				{
					ret += (first ? "" : (indent + baseIndent + (shorter ? "" : "]>")))
							+ ((MultiTreeMap) o).toString(indent + baseIndent, baseIndent, depth - 1, shorter);
					first = false;
				}
			}
		// if(ret.length() > 0)
		String sep = shorter ? "," : "\n";
		if(!ret.endsWith(sep))
			ret += sep;
		return ret;
	}
}
