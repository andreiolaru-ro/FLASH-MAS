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
package net.xqhs.flash.core;

import java.util.LinkedList;
import java.util.List;

/**
 * Types of categories in the configuration (some of which are entities).
 * <p>
 * Some categories may have names, and for some the name can be generated (if otherwise missing) from one or two parts
 * that are the values of specific attributes of the category. E.g. the name of a support infrastructure can be
 * auto-generated from its kind and its id, such as websocket:phones.
 * <p>
 * The second part of the name may be optional (this is default).
 * <p>
 * The parent of a category may be optional (this is default) or mandatory (the category <i>must</i> find itself within
 * the parent category.
 * 
 * @author andreiolaru
 */
public enum CategoryName {
	
	/**
	 * The node to which the configuration that follows belongs. An implicit NODE category is generated if no node entry
	 * is specified.
	 */
	NODE,
	
	/**
	 * The XML schema file against which to validate to deployment file (simple key, only first value is relevant).The
	 * schema element can only be taken from the XML and cannot be given at the command line.
	 */
	SCHEMA,
	
	/**
	 * The XML deployment file (simple key, only first value is relevant).
	 */
	DEPLOYMENT(NODE),
	
	/**
	 * General configuration settings, used by support infrastructures (hierarchical key).
	 */
	CONFIG(NODE),
	
	/**
	 * Java packages that contain classes needed in the deployment (simple key, all values are relevant).
	 */
	PACKAGE(NODE),
	
	/**
	 * Classes that are able to load various categories of elements in the configuration (hierarchical key).
	 * <p>
	 * Each hierarchical key in this subtree has a name that may have one part or two parts; a one-part name is the name
	 * of the loaded entity, a two-part name is the entity and the kind.
	 */
	LOADER("for", "kind", Is.OPTIONAL, NODE, Is.OPTIONAL),
	
	/**
	 * Support infrastructures used in the deployment (hierarchical key).
	 * <p>
	 * Each hierarchical key in this subtree has a name that may have one part or two parts; a one-part name is the kind
	 * of the support infrastructure, a two-part name is the kind and an identifier.
	 */
	SUPPORT("kind", "id", Is.OPTIONAL, NODE, Is.MANDATORY),
	
	/**
	 * Agents to create in the deployment, potentially inside particular support infrastructures (hierarchical key).
	 */
	AGENT(SUPPORT, Is.OPTIONAL),
	
	/**
	 * Features to be deployed in agents (hierarchical key).
	 */
	FEATURE(AGENT, Is.MANDATORY),
	
	;
	
	/**
	 * Values in this enumeration indicate whether a constraint is optional or mandatory.
	 */
	enum Is {
		/**
		 * Value indicates the constraint is optional.
		 */
		OPTIONAL,
		/**
		 * Value indicates the constraint is mandatory.
		 */
		MANDATORY,
		
		;
		
		/**
		 * @return <code>true</code> if the value is {@link Is#OPTIONAL}, <code>false</code> if {@link Is#MANDATORY}.
		 */
		boolean isOptional()
		{
			return this == OPTIONAL;
		}
	}
	
	/**
	 * The parent of the category.
	 */
	CategoryName	parent				= null;
	/**
	 * <code>false</code> if the category must necessarily appear inside its parent category; <code>true</code> if the
	 * category may also appear at top level.
	 */
	Is				optional_hierarchy	= Is.OPTIONAL;
	
	/**
	 * Element attribute giving the first part of the name of the element.
	 */
	String			nameAttribute1;
	
	/**
	 * Element attribute giving the second part of the name of the element.
	 */
	String			nameAttribute2;
	
	/**
	 * <code>true</code> if the first part of the name can be missing; <code>false</code> if the first part is
	 * mandatory.
	 */
	Is				optional_attribute1	= Is.OPTIONAL;
	
	/**
	 * Constructor for a top-level category.
	 */
	private CategoryName()
	{
	}
	
	/**
	 * Constructor for a category with a parent (hierarchy is mandatory).
	 * 
	 * @param _parent
	 *            - the parent category.
	 */
	private CategoryName(CategoryName _parent)
	{
		this(_parent, Is.OPTIONAL);
	}
	
	/**
	 * Constructor for a category that has a potentially optional parent.
	 * 
	 * @param _parent
	 *            - the parent.
	 * @param parent_optional
	 *            - <code>true</code> if hierarchy is optional.
	 */
	private CategoryName(CategoryName _parent, Is parent_optional)
	{
		this(null, null, Is.OPTIONAL, _parent, parent_optional);
		
	}
	
	/**
	 * Constructor for a category in which the name of elements if formed from one or two of the element attributes.
	 * 
	 * @param part1
	 *            - the attribute that gives the first part of the name.
	 * @param part2
	 *            - the attribute that gives the first part of the name.
	 * @param part1_optional
	 *            - <code>true</code> if the element can lack the first part of the name.
	 */
	private CategoryName(String part1, String part2, Is part1_optional)
	{
		this(part1, part2, part1_optional, null, Is.OPTIONAL);
	}
	
	/**
	 * Constructor for a category.
	 * 
	 * @param part1
	 *            - the attribute that gives the first part of the name.
	 * @param part2
	 *            - the attribute that gives the first part of the name.
	 * @param part1_optional
	 *            - <code>true</code> if the element can lack the first part of the name.
	 * @param _parent
	 *            - the parent.
	 * @param parent_optional
	 *            - <code>true</code> if hierarchy is optional.
	 */
	private CategoryName(String part1, String part2, Is part1_optional, CategoryName _parent, Is parent_optional)
	{
		nameAttribute1 = part1;
		nameAttribute2 = part2;
		optional_attribute1 = part1_optional;
		parent = _parent;
		optional_hierarchy = parent_optional;
	}
	
	/**
	 * @return the name of the category, in lower case.
	 */
	public String getName()
	{
		return this.name().toLowerCase();
	}
	
	/**
	 * @return the name of the parent category, if any was defined; <code>null</code> otherwise.
	 */
	public String getParent()
	{
		return parent != null ? parent.getName() : null;
	}
	
	/**
	 * @return <code>true</code> if hierarchy is optional.
	 */
	public boolean isParentOptional()
	{
		return optional_hierarchy.isOptional();
	}
	
	/**
	 * @return <code>true</code> if the elements in the category have names that get assembled from values of two
	 *         attributes.
	 */
	public boolean hasNameWithParts()
	{
		return nameAttribute1 != null && nameAttribute2 != null;
	}
	
	/**
	 * @return the names of the two attributes whose values form the element name.
	 */
	public String[] nameParts()
	{
		return new String[] { nameAttribute1, nameAttribute2 };
	}
	
	/**
	 * @return <code>true</code> if the first part of the name is optional.
	 */
	public boolean isNameFirstPartOptional()
	{
		return optional_attribute1.isOptional();
	}
	
	/**
	 * @return the hierarchical path of the category, from the parent to the farthest ancestor.
	 */
	List<String> getAncestorsList()
	{
		List<String> path = (parent == null) ? new LinkedList<String>() : parent.getAncestorsList();
		if(parent != null)
			path.add(0, parent.getName());
		return path;
	}
	
	/**
	 * @return the hierarchical path of the category, from the current category to the farthest ancestor.
	 */
	public String[] getAncestors()
	{
		return getAncestorsList().toArray(new String[0]);
	}

	/**
	 * Find the {@link CategoryName} identified by the given name.
	 * 
	 * @param name
	 *            - the name.
	 * @return the category.
	 */
	public static CategoryName byName(String name)
	{
		for(CategoryName s : CategoryName.values())
			if(s.getName().equals(name))
				return s;
		return null;
	}
}
