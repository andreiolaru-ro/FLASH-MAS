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
 * Some categories may have parents, and the parent of a category may be optional (this is default) or mandatory (the
 * category <i>must</i> find itself within the parent category).
 * <p>
 * Names of entities may sometimes identify those entities uniquely in the system (e.g. for agents, nodes, etc).
 * Entities which are in categories considered identifiable are not always required to have a name, but will be
 * identifiable only if they have one.
 * 
 * @author andreiolaru
 */
public enum CategoryName {
	
	/**
	 * The node to which the configuration that follows belongs. An implicit NODE category is generated if no node entry
	 * is specified.
	 */
	NODE(new CatPar().isIdentifiable()),
	
	/**
	 * The XML schema file against which to validate to deployment file (simple key, only first value is relevant).The
	 * schema element can only be taken from the XML and cannot be given at the command line.
	 */
	SCHEMA,
	
	/**
	 * The XML deployment file (simple key, only first value is relevant).
	 */
	DEPLOYMENT(new CatPar().hasParent(NODE)),
	
	/**
	 * General configuration settings, used by support infrastructures (hierarchical key).
	 */
	CONFIG(new CatPar().hasParent(NODE)),
	
	/**
	 * Java packages that contain classes needed in the deployment (simple key, all values are relevant).
	 */
	PACKAGE(new CatPar().hasParent(NODE)),
	
	/**
	 * Classes that are able to load various categories of elements in the configuration (hierarchical key).
	 * <p>
	 * Each hierarchical key in this subtree has a name that may have one part or two parts; a one-part name is the name
	 * of the loaded entity, a two-part name is the entity and the kind.
	 */
	LOADER(new CatPar().hasPartName("for", "kind", Is.OPTIONAL).hasParent(NODE, Is.OPTIONAL)),
	
	/**
	 * Support infrastructures used in the deployment (hierarchical key).
	 * <p>
	 * Each hierarchical key in this subtree has a name that may have one part or two parts; a one-part name is the kind
	 * of the support infrastructure, a two-part name is the kind and an identifier.
	 */
	SUPPORT(new CatPar().isIdentifiable().hasPartName("kind", "id", Is.OPTIONAL).hasParent(NODE, Is.MANDATORY)),
	
	/**
	 * Agents to create in the deployment, potentially inside particular support infrastructures (hierarchical key).
	 */
	AGENT(new CatPar().isIdentifiable().hasParent(SUPPORT, Is.OPTIONAL)),
	
	/**
	 * Features to be deployed in agents (hierarchical key).
	 */
	FEATURE(new CatPar().hasParent(AGENT, Is.MANDATORY)),
	
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
	 * Values in this enumeration indicate whether entities in a category are uniquely identifiable by their name, if
	 * any.
	 */
	enum NameIs {
		/**
		 * Value indicates that the name (if any) uniquely identifies the entity.
		 */
		IDENTIFIABLE,
		/**
		 * Value indicates that two entities with the same name may exist.
		 */
		NOT_IDENTIFIABLE,;
		
		/**
		 * @return <code>true</code> if value is {@link NameIs#IDENTIFIABLE}, <code>false</code> if
		 *         {@link NameIs#NOT_IDENTIFIABLE}.
		 */
		boolean isIdentifable()
		{
			return this == IDENTIFIABLE;
		}
	}
	
	/**
	 * Builder for category name parameters.
	 */
	private static class CatPar
	{
		/**
		 * Indicates whether the entity can be uniquely identified by its name, if any.
		 */
		NameIs			identifiable		= NameIs.NOT_IDENTIFIABLE;
		
		/**
		 * Element attribute giving the first part of the name of the element.
		 */
		String			nameAttribute1;
		
		/**
		 * Element attribute giving the second part of the name of the element.
		 */
		String			nameAttribute2;
		
		/**
		 * Indicates whether the first part of the name is optional.
		 */
		Is				optional_attribute1	= Is.OPTIONAL;
		
		/**
		 * The parent of the category.
		 */
		CategoryName	parent				= null;
		/**
		 * Indicates whether the category must necessarily appear inside its parent category.
		 */
		Is				optional_hierarchy	= Is.OPTIONAL;
		
		/**
		 * Default constructor, does nothing.
		 */
		public CatPar()
		{
			// nothing to do
		}
		
		/**
		 * Indicates a category in which entities are identifiable by name, if any.
		 * 
		 * @return the CatPar instance.
		 */
		CatPar isIdentifiable()
		{
			identifiable = NameIs.IDENTIFIABLE;
			return this;
		}
		
		/**
		 * Indicates a category with a parent (hierarchy is mandatory).
		 * 
		 * @param _parent
		 *            - the parent category.
		 * @return the CatPar instance.
		 */
		CatPar hasParent(CategoryName _parent)
		{
			return hasParent(_parent, Is.MANDATORY);
		}
		
		/**
		 * Indicates a category that has a potentially optional parent.
		 * 
		 * @param _parent
		 *            - the parent.
		 * @param parent_optional
		 *            - <code>true</code> if hierarchy is optional.
		 * @return the CatPar instance.
		 */
		CatPar hasParent(CategoryName _parent, Is parent_optional)
		{
			parent = _parent;
			optional_hierarchy = parent_optional;
			return this;
		}
		
		/**
		 * Indicates a category in which the name of elements if formed from one or two of the element attributes.
		 * 
		 * @param part1
		 *            - the attribute that gives the first part of the name.
		 * @param part2
		 *            - the attribute that gives the first part of the name.
		 * @param part1_optional
		 *            - <code>true</code> if the element can lack the first part of the name.
		 * @return the CatPar instance.
		 */
		CatPar hasPartName(String part1, String part2, Is part1_optional)
		{
			nameAttribute1 = part1;
			nameAttribute2 = part2;
			optional_attribute1 = part1_optional;
			return this;
		}
	}
	
	/**
	 * The parameters of the category.
	 */
	CatPar parameters;
	

	/**
	 * Constructor for a top-level category.
	 */
	private CategoryName()
	{
		this(new CatPar());
	}
	
	/**
	 * Constructor using parameters build through {@link CatPar}.
	 * 
	 * @param _parameters
	 *            - the {@link CatPar} instance.
	 */
	private CategoryName(CatPar _parameters)
	{
		parameters = _parameters;
	}
	
	/**
	 * @return the name of the category, in lower case.
	 */
	public String getName()
	{
		return this.name().toLowerCase();
	}
	
	/**
	 * @return <code>true</code> if the elements in the category are uniquely identifiable by their name, if any;
	 *         <code>false</code> otherwise.
	 */
	public boolean isIdentifiable()
	{
		return parameters.identifiable.isIdentifable();
	}
	
	/**
	 * @return the name of the parent category, if any was defined; <code>null</code> otherwise.
	 */
	public String getParent()
	{
		return parameters.parent != null ? parameters.parent.getName() : null;
	}
	
	/**
	 * @return <code>true</code> if hierarchy is optional.
	 */
	public boolean isParentOptional()
	{
		return parameters.optional_hierarchy.isOptional();
	}
	
	/**
	 * @return <code>true</code> if the elements in the category have names that get assembled from values of two
	 *         attributes.
	 */
	public boolean hasNameWithParts()
	{
		return parameters.nameAttribute1 != null && parameters.nameAttribute2 != null;
	}
	
	/**
	 * @return the names of the two attributes whose values form the element name.
	 */
	public String[] nameParts()
	{
		return new String[] { parameters.nameAttribute1, parameters.nameAttribute2 };
	}
	
	/**
	 * @return <code>true</code> if the first part of the name is optional.
	 */
	public boolean isNameFirstPartOptional()
	{
		return parameters.optional_attribute1.isOptional();
	}
	
	/**
	 * @return the hierarchical path of the category, from the parent to the farthest ancestor.
	 */
	List<String> getAncestorsList()
	{
		List<String> path = (parameters.parent == null) ? new LinkedList<>()
				: parameters.parent.getAncestorsList();
		if(parameters.parent != null)
			path.add(0, parameters.parent.getName());
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
