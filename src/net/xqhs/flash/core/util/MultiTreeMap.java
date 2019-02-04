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
 * The class acts as a tree of key-value pairs that allows multiple values for the same key. Keys are also called names.
 * <p>
 * The class builds on the same principles as {@link MultiValueMap} but acts rather like a hierarchical MultiValueMap.
 * It has two types of names.
 * <ul>
 * <li>Some names are "simple", in that they act exactly like names with String values in a {@link MultiValueMap}. There
 * is a name and one or more {@link String} values associated with the name.
 * <li>Other names are "hierarchical" (or tree-keys). Each hierarchical name has one or more associated
 * {@link MultiTreeMap} values.
 * </ul>
 * <p>
 * The class extends {@link MultiValueMap} such that only the two types of values are admitted -- {@link String} and
 * {@link MultiTreeMap}. One name can be associated wither with String values, or with tree values, but not with a
 * combination thereof (a name must either be simple or hierarchical).
 * <p>
 * There is no intersection allowed between the set of names for simple values and the set of types for tree values.
 * <p>
 * For clearer implementation and easier debugging, there are separate methods for the names which are supposed to only
 * hold one value (call them <i>singleton</i> names), both for simple and for tree names. It is possible to change the
 * singleton status of names by calling {@link #makeSingleton}.
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
	 * Internal method for adding a name (either simple or hierarchical).
	 * 
	 * @param name
	 *            - the name.
	 * @param isSimple
	 *            - <code>true</code> if the name is simple, <code>false</code> otherwise.
	 * @param isSingleton
	 *            - <code>true</code> if the name should have only one value.
	 */
	protected void addKey(String name, boolean isSimple, boolean isSingleton)
	{
		if(name != null && name.length() > padLen)
			padLen = name.length();
		if(isSimple)
			simpleKeys.add(name);
		else
			treeKeys.add(name);
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
			throw new IllegalArgumentException("Name [" + name
					+ "] is already present, as a hierarchical name. It cannot be assigned to simple (String) values.");
		if(!isSimple && simpleKeys.contains(name))
			throw new IllegalArgumentException(
					"Name [" + name + "] is already present, as a simple name. It cannot be assigned to tree values.");
		boolean keyExists = false;
		if(treeKeys.contains(name) || simpleKeys.contains(name))
		{ // existing key
			keyExists = true;
			if(isSingleton && !singletonKeys.contains(name))
				throw new IllegalArgumentException("Name [" + name
						+ "] is already present, as a multi (non-singleton) name. Multiple values cannot be added to it.");
			if(!isSingleton && singletonKeys.contains(name))
				throw new IllegalArgumentException("Name [" + name
						+ "] is already present, as a singleton name. Methods for singleton names cannot be used for it.");
		}
		else
			addKey(name, isSimple, isSingleton);
		if(isSingleton && keyExists)
			// replace value
			backingMap.get(name).set(0, value);
		else
			super.addObject(name, value);
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
	 *             if the given name is already present as a hierarchical name, or as a non-singleton name.
	 */
	public MultiTreeMap addSingleValue(String name, String value)
	{
		return addItem(name, value, true, true);
	}
	
	/**
	 * Associates an additional value with a name (or the first of several values).
	 * <p>
	 * When this method inserts a new name, use this if the name is expected to be associated with several values.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param value
	 *            - the value associated with the name.
	 * @return the instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the given name is already present as a hierarchical name, or as a singleton name.
	 */
	public MultiTreeMap addOneValue(String name, String value)
	{
		return addItem(name, value, true, false);
	}
	
	/**
	 * Alias of {@link #addOneValue(String, String)}, but should not be used because of its ambiguity.
	 */
	@Override
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
	 * Associates a singleton tree value to a hierarchical name .
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param tree
	 *            - the {@link MultiTreeMap} instance to associate with the name.
	 * @return this instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the given name is already present as a simple name or as a non-singleton name.
	 */
	public MultiTreeMap addSingleTree(String name, MultiTreeMap tree)
	{
		return addItem(name, tree, false, true);
	}
	
	/**
	 * Same as {@link #addSingleTree(String, MultiTreeMap)}, but instead of returning the original tree, it returns the
	 * newly added tree.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param tree
	 *            - the {@link MultiTreeMap} instance to associate with the name.
	 * @return the second argument.
	 */
	public MultiTreeMap addSingleTreeGet(String name, MultiTreeMap tree)
	{
		addItem(name, tree, false, true);
		return tree;
	}
	
	/**
	 * Associates an additional tree to a hierarchical name (or the first of several trees).
	 * <p>
	 * When this method inserts a new name, use this if the name is expected to be associated with several trees.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param tree
	 *            - the {@link MultiTreeMap} instance to associate with the name.
	 * @return this instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the given name is already present as a simple name or as a singleton name.
	 */
	public MultiTreeMap addOneTree(String name, MultiTreeMap tree)
	{
		return addItem(name, tree, false, false);
	}
	
	/**
	 * Same as {@link #addOneTree(String, MultiTreeMap)}, but instead of returning the original tree, it returns the
	 * newly added tree.
	 * 
	 * @param name
	 *            - the name (key) of the entry.
	 * @param tree
	 *            - the {@link MultiTreeMap} instance to associate with the name.
	 * @return the second argument.
	 */
	public MultiTreeMap addOneTreeGet(String name, MultiTreeMap tree)
	{
		addItem(name, tree, false, false);
		return tree;
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
	
	/**
	 * This method is not available in {@link MultiTreeMap}. One of the other addition methods must be used.
	 * 
	 * @throws UnsupportedOperationException
	 *             always.
	 */
	@Deprecated
	@Override
	public MultiTreeMap addObject(String name, Object value)
	{
		throw new UnsupportedOperationException("The MultiTreeMap class does not allow adding objects arbitrarily.");
	}
	
	/**
	 * Replaces or adds the given value for the given singleton name.
	 * 
	 * @param name
	 *            - the (key) name of the entry.
	 * @param value
	 *            - the new value.
	 * @return this instance itself, for chained calls.
	 * @throws IllegalArgumentException
	 *             if the name exist and is not simple or is not singleton.
	 */
	public MultiTreeMap setValue(String name, String value)
	{
		return addItem(name, value, true, true);
	}
	
	/**
	 * Removes all values associated with a name but does not remove the name from the set, so it stays in the same
	 * position as it was added.
	 * <p>
	 * If the name did not previously exist, it is not created.
	 * <p>
	 * The operation is only available for non-singleton names. For singleton names, {@link #setValue} with
	 * <code>null</code>.
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
			throw new IllegalArgumentException("Singleton name [" + name + "] cannot be cleared.");
		if(backingMap.containsKey(name))
			backingMap.get(name).clear();
		return this;
	}
	
	/**
	 * Changes the status of a name from singleton to non-singleton or back.
	 * 
	 * @param name
	 *            - the name to change.
	 * @param makeSingleton
	 *            - <code>true</code> if the name should be made singleton; <code>false</code> if it should be made
	 *            non-singleton.
	 * @return the instance itself.
	 * @throws IllegalArgumentException
	 *             if the name does not exist.
	 * @throws IllegalStateException
	 *             if the name is not singleton and the request was to make it singleton, but it is already associated
	 *             with more than one value.
	 */
	public MultiTreeMap makeSingleton(String name, boolean makeSingleton)
	{
		if(!containsKey(name))
			throw new IllegalArgumentException("The name [" + name + "] does not exist.");
		if(!singletonKeys.contains(name) && makeSingleton && backingMap.get(name).size() > 1)
			throw new IllegalStateException("The name [" + name + "] contains more than 1 value.");
		if(singletonKeys.contains(name) && !makeSingleton)
			singletonKeys.remove(name);
		if(!singletonKeys.contains(name) && makeSingleton)
			singletonKeys.add(name);
		return this;
	}
	
	/**
	 * @param name
	 *            - the name to search.
	 * @return <code>true</code> if the name is contained as a singleton name; <code>false</code> otherwise.
	 */
	public boolean isSingleton(String name)
	{
		return singletonKeys.contains(name);
	}
	
	/**
	 * @param name
	 *            - the name to search.
	 * @return <code>true</code> if the name has been added to this instance as a simple name; <code>false</code>
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
	 * @return <code>true</code> if the name has been added to this instance as a simple name; <code>false</code>
	 *         otherwise.
	 */
	public boolean containsSimpleName(String name)
	{
		return isSimple(name);
	}
	
	/**
	 * @param name
	 *            - the name to search.
	 * @return <code>true</code> if the name has been added to this instance as a hierarchical name; <code>false</code>
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
	 * @return <code>true</code> if the name has been added to this instance as a hierarchical name; <code>false</code>
	 *         otherwise.
	 */
	public boolean containsHierarchicalName(String name)
	{
		return isHierarchical(name);
	}
	
	/**
	 * @return the list of simple names, as a new list, not backed by this map.
	 */
	public List<String> getSimpleNames()
	{
		return new LinkedList<>(simpleKeys);
	}
	
	/**
	 * @return the list of hierarchical names, as a new list, not backed by this map.
	 */
	public List<String> getHierarchicalNames()
	{
		return new LinkedList<>(treeKeys);
	}
	
	/**
	 * Alias for {@link #getHierarchicalNames()}.
	 * 
	 * @return the list of hierarchical names.
	 */
	public List<String> getTreeKeys()
	{
		return getHierarchicalNames();
	}
	
	/**
	 * Method to be called for any get operation; verifies the existence of a name, as well as the compatibility between
	 * the method used (as indicated by the second and third arguments) and the actual status of the name.
	 * 
	 * @param name
	 *            - the name to check.
	 * @param asSimple
	 *            - <code>true</code> if the name should be viewed as a simple name.
	 * @param asSingleton
	 *            - <code>true</code> if the name should be viewed as a singleton name.
	 * @return <code>true</code> if the key exists and its status corresponds to the given arguments; <code>false</code>
	 *         if the key does not exist.
	 * @throws IllegalArgumentException
	 *             if the name exists but does not correspond to the specified access.
	 */
	protected boolean checkKeyAccess(String name, boolean asSimple, boolean asSingleton)
	{
		if(!containsKey(name))
			return false;
		if(asSimple && !simpleKeys.contains(name))
			throw new IllegalArgumentException("Name [" + name + "] is not a simple name.");
		if(!asSimple && !treeKeys.contains(name))
			throw new IllegalArgumentException("Name [" + name + "] is not a hierarchical name.");
		if(asSingleton && !singletonKeys.contains(name))
			throw new IllegalArgumentException("Name [" + name + "] is not a singleton name.");
		if(!asSingleton && singletonKeys.contains(name))
			throw new IllegalArgumentException("Name [" + name + "] is a singleton name.");
		return true;
	}
	
	/**
	 * In this implementation, {@link #get(String)} is an alias of {@link #getFirstValue(String)}, so it must only be
	 * used for non-singleton simple values.
	 */
	@Override
	public String get(String name)
	{
		checkKeyAccess(name, true, false);
		return super.get(name);
	}
	
	/**
	 * In this implementation, {@link #getValue(String)} is an alias of {@link #getFirstValue(String)}, so it must only
	 * be used for non-singleton simple values.
	 */
	@Override
	public String getValue(String name)
	{
		return get(name);
	}
	
	/**
	 * Get the value associated with a singleton name.
	 * 
	 * @param name
	 *            - the name (key) tos search.
	 * @return the value associated with the name. may be <code>null</code> if the name does not exist, if the name has
	 *         been {@link #clear}ed or {@link #setValue} was called with <code>null</code>.
	 * 
	 * @throws IllegalArgumentException
	 *             if the name is used for a non-singleton name or for a hierarchical name.
	 */
	public String getSingleValue(String name)
	{
		checkKeyAccess(name, true, true);
		return (backingMap.containsKey(name) && backingMap.get(name).size() == 1) ? (String) backingMap.get(name).get(0)
				: null;
	}
	
	/**
	 * Return the first value associated with a non-singleton name. May return <code>null</code> if the name does not
	 * exist or has been {@link #clear}ed.
	 * 
	 * @param name
	 *            - the name (key) to search.
	 * @return the first value associated with the name.
	 * @throws IllegalArgumentException
	 *             if the name is used for a singleton name or for a hierarchical name.
	 */
	public String getFirstValue(String name)
	{
		return get(name);
	}
	
	/**
	 * Retrieves all simple (String) values associated with the given name (key).
	 * 
	 * @throws IllegalArgumentException
	 *             if the name is used for a singleton name or for a hierarchical name.
	 */
	@Override
	public List<String> getValues(String name)
	{
		checkKeyAccess(name, true, false);
		return super.getValues(name);
	}
	
	/**
	 * Retrieves the tree associated with the given singleton name.
	 * 
	 * @param name
	 *            - the name (key) to search.
	 * @return the associated tree.
	 */
	public MultiTreeMap getSingleTree(String name)
	{
		checkKeyAccess(name, false, true);
		return getCreateTree(name, false, true);
	}
	
	/**
	 * Retrieves the tree associated with the given singleton name.
	 * <p>
	 * Optionally, if no such tree exists, creates one, adds it as a value for the given name and returns it.
	 * 
	 * @param name
	 *            - the name (key) to search.
	 * @param create
	 *            - if <code>true</code> and no tree is associated with the name, a new tree is added for the given name
	 *            and returned. If <code>false</code>, the method is the same as {@link #getSingleTree(String)}.
	 * @return the associated tree.
	 */
	public MultiTreeMap getSingleTree(String name, boolean create)
	{
		return getCreateTree(name, create, true);
	}
	
	/**
	 * Retrieves the first tree associated with the given non-singleton name.
	 * 
	 * @param name
	 *            - the name (key) to search.
	 * @return the first associated tree.
	 */
	public MultiTreeMap getFirstTree(String name)
	{
		checkKeyAccess(name, false, false);
		return getCreateTree(name, false, false);
	}
	
	/**
	 * Retrieves the first tree associated with the given non-singleton name.
	 * 
	 * @param name
	 *            - the name (key) to search.
	 * @param create
	 *            - if <code>true</code> and no tree is associated with the name, a new tree is added for the given name
	 *            and returned. If <code>false</code>, the method is the same as {@link #getFirstTree(String)}.
	 * @return the first associated tree.
	 */
	public MultiTreeMap getFirstTree(String name, boolean create)
	{
		return getCreateTree(name, create, false);
	}
	
	/**
	 * Retrieves all tree values associated with the given non-singleton name (key). May return <code>null</code> if the
	 * name has been {@link #clear}ed.
	 * 
	 * @param name
	 *            - the name.
	 * @return the trees associated with the name.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given name is a simple name (its values are not trees).
	 */
	public List<MultiTreeMap> getTrees(String name)
	{
		checkKeyAccess(name, false, false);
		List<MultiTreeMap> ret = new LinkedList<>();
		for(Object t : backingMap.get(name))
			ret.add((MultiTreeMap) t);
		return ret;
	}
	
	/**
	 * Retrieves the first tree associated with the given name.
	 * <p>
	 * Optionally, if no such tree exists, creates one, adds it as a value for the given name and returns it.
	 * 
	 * @param name
	 *            - the name (key) to search.
	 * @param create
	 *            - if <code>true</code> and no tree is associated with the name, a new tree is added for the given name
	 *            and returned. If <code>false</code>, the method is the same as {@link #getSingleTree(String)} or
	 *            {@link #getFirstTree(String)}, depending on the value of the last argument.
	 * @param isSingletonName
	 *            - if the previous argument is <code>true</code>, this arguments indicates whether the newly created
	 *            name should be created as a singleton name (this argument should be <code>true</code>) or not (this
	 *            argument should be <code>false</code>). If the previous argument is <code>false</code>, this indicates
	 *            whether the name should be considered as singleton or not.
	 * @return the first associated tree.
	 */
	protected MultiTreeMap getCreateTree(String name, boolean create, boolean isSingletonName)
	{
		if(!containsKey(name) && create)
		{
			MultiTreeMap newTree = new MultiTreeMap();
			addItem(name, newTree, false, isSingletonName);
			return newTree;
		}
		checkKeyAccess(name, false, isSingletonName);
		return (MultiTreeMap) super.getObject(name);
	}
	
	/**
	 * Get the value at the end of a path in a tree. The last name must be a simple name. All other names must be
	 * hierarchical names. For multiple values, only the first value is checked.
	 * <p>
	 * The method fails fast: if one of the names (except for the last one) is not a hierarchical name, the method
	 * returns <code>null</code>.
	 * 
	 * @param names
	 *            - the path, consisting of names.
	 * @return the value associated with the leaf at the end of the path, if any is found; <code>null</code> otherwise.
	 */
	public String getDeepValue(String... names)
	{
		switch(names.length)
		{
		case 0:
			return null;
		case 1:
			if(containsKey(names[0]))
				return isSingleton(names[0]) ? getSingleValue(names[0]) : getFirstValue(names[0]);
			return null;
		default:
			if(!isHierarchical(names[0]))
				return null;
			return (isSingleton(names[0]) ? getSingleTree(names[0]) : getFirstTree(names[0]))
					.getDeepValue(Arrays.copyOfRange(names, 1, names.length));
		}
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
		for(String name : backingMap.keySet())
			if(simpleKeys.contains(name))
			{
				if(shorter)
					continue;
				ret += (justtree || shorter ? "" : "\n") + indent
						+ String.format("%-" + (padLen + 4) + "s", "[" + name + ("]" + (isSingleton(name) ? ":" : ">")))
						+ backingMap.get(name);
				justtree = false;
			}
			else
			{
				ret += (justtree || shorter ? "" : "\n") + indent + (shorter ? "" : "[") + name
						+ (shorter ? "" : ("]" + (isSingleton(name) ? "" : ">")));
				justtree = true;
				boolean first = true;
				for(Object o : backingMap.get(name))
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
