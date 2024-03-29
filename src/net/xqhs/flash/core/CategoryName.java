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
package net.xqhs.flash.core;

import java.util.LinkedList;
import java.util.List;

/**
 * Types of categories in the configuration (some of which are entities).
 * <p>
 * TODO> rephrase and complete this documentation, with bullets for each property.
 * <p>
 * Some categories may have names, and for some the name can be generated (if otherwise missing) from one or two parts
 * that are the values of specific attributes of the category. E.g. the name of a support infrastructure can be
 * auto-generated from its kind and its id, such as websocket:phones.
 * <p>
 * The second part of the name may be optional (this is default).
 * <p>
 * Some categories may have parents, and the parent of a category may be optional (this is default) or mandatory (the
 * category <i>must</i> find itself within the parent category). Optional parents are useful when reading text input,
 * such that entities are placed inside their parents, if they exist.
 * <p>
 * Some categories may be unique to their context, meaning a new occurrence of the category erases / overwrites any
 * previous occurrences in the same context.
 * <p>
 * Names of entities may sometimes identify those entities uniquely in the system (e.g. for agents, nodes, etc).
 * Entities which are in categories considered identifiable are not always required to have a name, but will be
 * identifiable only if they have one.
 * 
 * @author andreiolaru
 */
public enum CategoryName {
	/**
	 * The entire deployment, which may contain multiple nodes.
	 */
	DEPLOYMENT(new CatPar().isUnique()),
	
	/**
	 * The node to which the configuration that follows belongs. An implicit NODE category is generated if no node entry
	 * is specified.
	 */
	NODE(new CatPar().isIdentifiable().hasParent(DEPLOYMENT)),
	/**
	 * Support infrastructures used in the deployment (hierarchical key).
	 * <p>
	 * Each hierarchical key in this subtree has a name that may have one part or two parts; a one-part name is the kind
	 * of the support infrastructure, a two-part name is the kind and an identifier.
	 */
	PYLON(new CatPar().isIdentifiable().hasPartName("kind", "id", Is.OPTIONAL).hasParent(NODE)
			.isPortableFrom(DEPLOYMENT)),
	/**
	 * Agents to create in the deployment, potentially inside particular support infrastructures (hierarchical key).
	 */
	AGENT(new CatPar().isIdentifiable().hasPartName("kind", "id", Is.OPTIONAL).hasParent(PYLON)),
	/**
	 * Shards to be deployed in agents (hierarchical key).
	 */
	SHARD(new CatPar().hasParent(AGENT).isPortableFrom(DEPLOYMENT)),
	
	/**
	 * The XML schema file against which to validate to deployment file (simple key, only first value is relevant).The
	 * schema element can only be taken from the XML and cannot be given at the command line.
	 */
	SCHEMA(new CatPar().isValue().isUnique()),
	/**
	 * The XML deployment file (simple key, only first value is relevant).
	 */
	DEPLOYMENT_FILE(new CatPar().isValue().isUnique().hasParent(NODE).isPortableFrom(DEPLOYMENT)),
	/**
	 * Java packages that contain classes needed in the deployment (simple key, all values are relevant).
	 */
	PACKAGE(new CatPar().isValue().isPortableFrom(DEPLOYMENT, CatPar.VISIBLE_ON_PATH)),
	/**
	 * The entities to load and their order (entity names, lower-case, separated by
	 * {@link DeploymentConfiguration#LOAD_ORDER_SEPARATOR}).
	 */
	LOAD_ORDER(new CatPar().isValue().isUnique().hasParent(NODE).isPortableFrom(DEPLOYMENT)),
	
	/**
	 * Classes that are able to load various categories of elements in the configuration (hierarchical key).
	 * <p>
	 * Each hierarchical key in this subtree has a name that may have one part or two parts; a one-part name is the name
	 * of the loaded entity, a two-part name is the entity and the kind.
	 */
	LOADER(new CatPar().isIdentifiable().isNotEntity().hasPartName("for", "kind", Is.OPTIONAL).hasParent(NODE)
			.isPortableFrom(DEPLOYMENT)),
	
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
		// /**
		// * Used as argument in {@link #hasParent(CategoryName, boolean)}
		// */
		// public static final boolean CAN_ADD_AUTO = true;
		
		/**
		 * Used as argument in
		 */
		public static final boolean	VISIBLE_ON_PATH		= true;
		
		/**
		 * Indicates whether an entry in the deployment configuration is a simple value.
		 */
		boolean						isValue				= false;
		/**
		 * Indicates whether an entry in the deployment configuration is not an entity and other entities should not be added within.
		 */
		boolean						isNotEntity				= false;		
		/**
		 * Indicates whether the entity can be uniquely identified by its name, if any.
		 */
		NameIs						identifiable		= NameIs.NOT_IDENTIFIABLE;
		// /**
		// * Indicates that an instance of the entity can be generated if required as a mandatory parent for some other
		// * entity.
		// */
		// boolean canBeAutoGenerated = false;
		
		/**
		 * Indicates whether at most one instance of this category is allowed.
		 */
		boolean						isUnique			= false;
		
		/**
		 * Element attribute giving the first part of the name of the element.
		 */
		String						nameAttribute1;
		
		/**
		 * Element attribute giving the second part of the name of the element.
		 */
		String						nameAttribute2;
		
		/**
		 * Indicates whether the second part of the name is optional.
		 */
		Is							optional_attribute2	= Is.OPTIONAL;
		
		/**
		 * The parent of the category.
		 */
		CategoryName				parent				= null;
		// /**
		// * Indicates whether the entity can be automatically added as child of a sibling, if an adequate sibling
		// exists
		// */
		// boolean parent_autoadd = false;
		
		/**
		 * Indicates that the entity can be declared inside this category and then it will be ported to its parent.
		 */
		CategoryName				portableFrom		= null;
		
		/**
		 * For portable categories, indicates that it should be added to all entities between where it was declared and
		 * the actual parent.
		 */
		boolean						visibleOnPath		= false;
		
		/**
		 * No-argument constructor, does nothing.
		 */
		public CatPar()
		{
			// nothing to do
		}
		
		/**
		 * Indicates the category is a simple value. It imples that it cannot be an entity.
		 * 
		 * @return the CatPar instance.
		 */
		CatPar isValue()
		{
			isValue = true;
			isNotEntity = true;
			return this;
		}
		
		/**
		 * Indicates the category is not an entity.
		 * 
		 * @return the CatPar instance.
		 */
		CatPar isNotEntity() {
			isNotEntity = true;
			return this;
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
		
		// /**
		// * Indicates that an instance of the entity can be generated if required as a mandatory parent for some other
		// * entity.
		// *
		// * @return the CatPar instance.
		// */
		// CatPar canBeAutoGenerated()
		// {
		// canBeAutoGenerated = true;
		// return this;
		// }
		
		/**
		 * Indicates a category which can appear at most once in its context.
		 * 
		 * @return the CatPar instance.
		 */
		CatPar isUnique()
		{
			isUnique = true;
			return this;
		}
		
		/**
		 * Indicates a category that has a potentially optional parent.
		 * 
		 * @param _parent
		 *            - the parent.
		 * @return the CatPar instance.
		 */
		CatPar hasParent(CategoryName _parent)
		{
			parent = _parent;
			return this;
		}
		
		// /**
		// * Indicates a category that has a potentially optional parent, which can potentially be auto-added (see
		// * #CAN_ADD_AUTO).
		// *
		// * @param _parent
		// * - the parent.
		// * @param autoadd
		// * - <code>true</code> if the entity can be automatically added as child of a sibling, if an adequate
		// * sibling exists.
		// * @return the CatPar instance.
		// */
		// CatPar hasParent(CategoryName _parent, boolean autoadd)
		// {
		// parent = _parent;
		// parent_autoadd = autoadd;
		// return this;
		// }
		
		/**
		 * Indicates a category in which the name of elements if formed from one or two of the element attributes.
		 * 
		 * @param part1
		 *            - the attribute that gives the first part of the name.
		 * @param part2
		 *            - the attribute that gives the second part of the name.
		 * @param part2_optional
		 *            - <code>true</code> if the element can lack the second part of the name.
		 * @return the CatPar instance.
		 */
		CatPar hasPartName(String part1, String part2, Is part2_optional)
		{
			nameAttribute1 = part1;
			nameAttribute2 = part2;
			optional_attribute2 = part2_optional;
			return this;
		}
		
		/**
		 * Indicates that the entity can be declared inside this category and then it will be ported to its parent.
		 * 
		 * @param origin
		 *            - the highest-level entity inside which this entity can be declared.
		 * @return the CatPar instance.
		 */
		CatPar isPortableFrom(CategoryName origin)
		{
			return isPortableFrom(origin, false);
		}
		
		/**
		 * Indicates that the entity can be declared inside this category and then it will be ported to its parent.
		 * 
		 * @param origin
		 *            - the highest-level entity inside which this entity can be declared.
		 * @param _visibleOnPath
		 *            - indicates that it should be added to all entities between where it was declared and the actual
		 *            parent; use {@link CatPar#VISIBLE_ON_PATH} .
		 * @return the CatPar instance.
		 */
		CatPar isPortableFrom(CategoryName origin, boolean _visibleOnPath)
		{
			portableFrom = origin;
			visibleOnPath = _visibleOnPath;
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
	 * This method is an alias of {@link #getName()}.
	 * 
	 * @return the name of the category.
	 */
	public String s()
	{
		return getName();
	}
	
	/**
	 * @return <code>true</code> if category is a simple value; <code>false</code> if category is a hierarchical
	 *         element.
	 */
	public boolean isValue()
	{
		return parameters.isValue;
	}
	
	/**
	 * @return <code>true</code> if category cannot be an entity; <code>false</code> if category can contain other
	 *         categories and entities.
	 */
	public boolean isNotEntity()
	{
		return parameters.isNotEntity;
	}
	
	/**
	 * @return <code>true</code> if the elements in the category are uniquely identifiable by their name, if any;
	 *         <code>false</code> otherwise.
	 */
	public boolean isIdentifiable()
	{
		return parameters.identifiable.isIdentifable();
	}
	
	// /**
	// * @return <code>true</code> if an instance of the entity can be generated if required as a mandatory parent for
	// * some other entity.
	// */
	// public boolean canBeAutoGenerated()
	// {
	// return parameters.canBeAutoGenerated;
	// }
	
	/**
	 * @return <code>true</code> if at most one instance of this category should exist in its context.
	 */
	public boolean isUnique()
	{
		return parameters.isUnique;
	}
	
	/**
	 * @return the name of the parent category, if any was defined; <code>null</code> otherwise.
	 */
	public String getParent()
	{
		return parameters.parent != null ? parameters.parent.getName() : null;
	}
	
	// /**
	// * @return <code>true</code> if the entity can be automatically added as child of a sibling, if an adequate
	// sibling
	// * exists
	// */
	// public boolean canAutoAddParent()
	// {
	// return parameters.parent_autoadd;
	// }
	
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
	 * @return <code>true</code> if the second part of the name is optional.
	 */
	public boolean isNameSecondPartOptional()
	{
		return parameters.optional_attribute2.isOptional();
	}
	
	/**
	 * @return the hierarchical path of the category, from the parent to the farthest ancestor.
	 */
	public List<String> getAncestorsList()
	{
		List<String> path = (parameters.parent == null) ? new LinkedList<>() : parameters.parent.getAncestorsList();
		if(parameters.parent != null)
			path.add(0, parameters.parent.getName());
		return path;
	}
	
	/**
	 * @return the hierarchical path of the category, from the parent to the farthest ancestor.
	 */
	public String[] getAncestors()
	{
		return getAncestorsList().toArray(new String[0]);
	}
	
	/**
	 * @return the highest-level category inside which this entity can be declared, after which it will be automatically
	 *         ported to its parent entity.
	 */
	public CategoryName portableFrom()
	{
		return parameters.portableFrom;
	}
	
	/**
	 * @return <code>true</code> if the category should be ported to all entities on the path to its declared parent.
	 */
	public boolean visibleOnPath()
	{
		return parameters.visibleOnPath;
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
