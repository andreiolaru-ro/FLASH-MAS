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
 * 
 * The class builds on the same principles as {@link ParameterSet} but acts rather like a hierarchical ParameterSet. It
 * has two types of keys.
 * <ul>
 * <li>Some keys are "simple", in that they act exactly like keys with String values in a {@link ParameterSet}. There is
 * a name and one or more String values associated with the name.
 * <li>Other keys are "hierarchical" (or tree-keys). Each tree key has a name and an associated {@link TreeParameterSet}
 * value.
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
	 * The class UID
	 */
	private static final long	serialVersionUID	= 4361796924244682172L;
	
	/**
	 * Keys that have String values.
	 */
	protected Set<String>		simpleKeys			= new HashSet<>();
	/**
	 * Keys that have {@link TreeParameterSet} values.
	 */
	protected Set<String>		treeKeys			= new HashSet<>();
	
	/**
	 * Is updated with the longest length of a key; used for pretty printing without calculating the longest key every
	 * time.
	 */
	protected int				padLen				= 0;
	
	/**
	 * Internal method for adding a key (either simple or hierarchical.
	 * 
	 * @param name
	 *            - the key.
	 * @param isSimple
	 *            - <code>true</code> if the key is simple, <code>false</code> otherwise.
	 */
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
		if(treeKeys.contains(name))
			throw new IllegalArgumentException("Key " + name
					+ " is already present, as a hierarchical key. It cannot be assigned to simple (String) values");
		addKey(name, true);
		return (TreeParameterSet) super.add(name, value);
	}
	
	@Override
	public TreeParameterSet addAll(String name, List<String> values)
	{
		if(treeKeys.contains(name))
			throw new IllegalArgumentException("Key " + name
					+ " is already present, as a hierarchical key. It cannot be assigned to simple (String) values");
		addKey(name, true);
		return (TreeParameterSet) super.addAll(name, values);
	}
	
	/**
	 * Adds a tree value for a type-key pair.
	 * 
	 * @param name
	 *            - the key.
	 * @param tree
	 *            - the {@link TreeParameterSet} instance to assign to the key.
	 * @return this instance itself, for chained calls.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given key is already present as a simple key.
	 */
	public TreeParameterSet addTree(String name, TreeParameterSet tree)
	{
		if(simpleKeys.contains(name))
			throw new IllegalArgumentException(
					"Key " + name + " is already present, as a simple key. It cannot be assigned to tree values");
		treeKeys.add(name);
		return (TreeParameterSet) super.addObject(name, tree);
	}
	
	@Override
	public TreeParameterSet addObject(String name, Object value)
	{
		if(value == null || value instanceof String || value instanceof TreeParameterSet)
			return (TreeParameterSet) super.addObject(name, value);
		throw new UnsupportedOperationException(
				"The TreeParameterSet class does not allow adding objects of arbitrary types.");
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
		if(treeKeys.contains(name))
			throw new IllegalArgumentException("Key " + name
					+ " is already present, as a hierarchical key. It cannot be assigned to simple (String) values");
		if(super.get(name) != null)
			parameterSet.remove(name);
		addKey(name, true);
		return (TreeParameterSet) super.add(name, value);
	}
	
	/**
	 * Removes all values associated with a key but does not remove the key from the set, so it stays in the same
	 * position as it was added.
	 * 
	 * @param name
	 *            - the key to clear.
	 * @return the instance itself, for chained calls.
	 */
	public TreeParameterSet clear(String name)
	{
		parameterSet.get(name).clear();
		return this;
	}
	
	/**
	 * @param name
	 *            - the key.
	 * @return <code>true</code> if the name has been added to this instance as a simple key; <code>false</code>
	 *         otherwise.
	 */
	public boolean isSimple(String name)
	{
		return simpleKeys.contains(name);
	}
	
	/**
	 * @param name
	 *            - the key.
	 * @return <code>true</code> if the name has been added to this instance as a hierarchical key; <code>false</code>
	 *         otherwise.
	 */
	public boolean isHierarchical(String name)
	{
		return treeKeys.contains(name);
	}
	
	/**
	 * @return the list of simple keys.
	 */
	public List<String> getSimpleKeys()
	{
		return new LinkedList<>(simpleKeys);
	}
	
	/**
	 * @return the list of hierarchical keys.
	 */
	public List<String> getHierarchicalKeys()
	{
		return new LinkedList<>(treeKeys);
	}
	
	/**
	 * Alias for {@link #getHierarchicalKeys()}.
	 * 
	 * @return the list of hierarchical keys.
	 */
	public List<String> getTreeKeys()
	{
		return getHierarchicalKeys();
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
	public List<TreeParameterSet> getTrees(String name)
	{
		if(simpleKeys.contains(name))
			throw new IllegalArgumentException("Key " + name + " is not a hierarchical key.");
		List<TreeParameterSet> ret = new LinkedList<>();
		for(Object t : getObjects(name))
			ret.add((TreeParameterSet) t);
		return ret;
	}
	
	/**
	 * Retrieves the first tree associated with the given name.
	 * 
	 * @param name
	 *            - the name (key).
	 * @return the first associated tree.
	 */
	public TreeParameterSet getTree(String name)
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
	public TreeParameterSet getTree(String name, boolean create)
	{
		List<TreeParameterSet> trees = getTrees(name);
		if(trees.isEmpty())
		{
			if(create)
			{
				TreeParameterSet newTree = new TreeParameterSet();
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
							+ ((TreeParameterSet) o).toString(indent + baseIndent, baseIndent, depth - 1, shorter);
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
