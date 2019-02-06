<!-- ---------------------------------------------
Copyright (C) 2018 Andrei Olaru.

This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.

Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
--------------------------------------------- -->


"Easy to beginners, powerful to experts"

**Current target:**
  * deploy a composite agent
    * what do about having support implementations when pre-loading features
      * should we leave loading (and finding) features to the composite agent, till after the agent is added all contexts? (who adds contexts?)
  * rename TreeParameterSet and ParameterSet
  * manage portables for CLI entries
		
  * implement getAppropriateTree / addAppropriateTree methods


Concept Names
=============

  * simulation -> deployment
  * visualization -> monitoring / control
  * component -> feature -> shard
  * platform -> support
  
  * ParameterSet -> ListMap
  * TreeParameterSet -> TreesMap

  * Support pilons (or pilons, fo short?)

Services / features
===================

  * each feature uses one service
  * one support may offer multiple services
  * support may recommend feature implementations



Configuration
=============

Assembling parts to form a name would be done *only* for entities for which this union can stand for its identifier.

Unnamed entities are allowed, but cannot be referenced (such as for <in-context-of>).

There is a root category, namely DEPLOYMENT. If no NODE is specified, a default one will be created for entities that need to be inside a node (e.g. entities, agents, etc). This implicit node will be the first node among the nodes with no name. If a node with no name has already been introduced, it will be the same one.

The local node is the first node not specifically designated as remote.

Context
-------
  * context visibility for entity A:
    * an entity is visible to its direct child **DONE**
    * if entity B has entity A as parent, then it is visible to entity B (due to declaration order, cannot declare an entity as visible to another entity which has it as parent
    * an entity A, marked as visible to an entity B
      * is visible to any descendant entity B
      * if there is an (registered) entity C, ancestor of entity B, and descendant of entity A (in the category hierarchy)
        * A is visible to C and to any entity that is an (actual) ancestor of C
        * if there is no B descendant to entity A, A is visible to the deepest registered entity C, as defined above
  * TODO: further test correct addition of context

XML
---

Each SettingsName contains information on how to assemble elements from the XML into a name, if necessary. E.g. support kind and id can be assembled into kind:id.


CLI
---

all name:val pairs belong in an element

all elements belong in a category; all categories belong in elements or at the root level

the root level is the local node, which may not be specifically identified. Lacking a specific "-node name" element introduction, a name for it will be automatically generated
  * this way, remote nodes may be specified as well.
  * the first node is the local node.

	CLI arguments			 ::= scenario-file? category-description*
	category-description	 ::= -category element element_description
	element_description	 ::= (par:val | par)* category-description* [if just par, value will be null]
	element					 ::= 		// basically anything, but interpreted as:
								part1:part2		[depending on category, can mean loader:id or type:id category:type]
								type:				[an unnamed element with the specified type/loader]
								name				[depending on category, a named element of the default type or an unnamed element with this type]
														[the exact variant is decided in Boot/NodeLoader, not in the CLI parser]
													

Fusion
------

Cases where one of the sources contains only a partially specified name are managed by the specific loader or entity that uses that configuration.

TODO
----
  * check duplicate names for identifiable entities
  * implement visibleTo property in the deployment input, write visibility data into individual nodes
  * implement identifiable property in the deployment input?
  * protect agents from intrusive / malicious shards?
  * multiple shards per type
   


Future
------
  * introduce entities with required name / required kind
  * should an empty no-attributes agent tag be admissible? In what scenario?
  
  * -i for interactive console which allows adding (and removing) support / agents / other entities
  * -select to select the local node configuration in the deployment file (e.g. have the same deployment file for multiple nodes and just select different local nodes)
  
  * introduce tree control: "<categ" goes back to categ and << goes to root (above nodes) //>
  * between a category and its mandatory parent there may also be other categories (use case: agent arrays as entities that can be managed as a whole)
  * fuse trees from XML and CLI in the case of elements with optional parents
  * implement _q_ attribute in XML that is parsed by the CLI parser
  * implement special categories such as
    * -all-of:always category par:val+ (copy/fuse the subordinate tree to all children of category)
    * -all-of:missing category par:val+ (insert the values in the subordinate tree in all children of category, but only if no such parameter is already defined).



Loading / Boot
==============

Deployment configuration contains:
  * node information
    * Schema & deployment XML (simple keys, first value counts)
    * general configuration settings (e.g. network configs) for this node (tree key)
    * the load order -- what entities to load and order in which to load entities
    * package list (simple key, all values are relevant)
    * loaders (specified for entities and kinds)
    * support infrastructures
      * agents
      * features
    * other entities

  * categories that are simple values will be overwritten, not added to (e.g. for load_order).
  

Entities
--------
  * Entities extend Entity
  * Entity constructors should receive a ParamSet as argument or should implement ConfigurableEntity, which works more like a config 
  * Entities should be loadable through SimpleLoader (which supports both methods above), and only for special cases (where a factory is needed) should specialized loaders be used
  	* chose to use a loader for CompositeAgents so we can test features (existence / pre-loading) beforehand
  * Entity context
    * is all entities containing it, although the one specified in the class/interface is still special.
    * Future: be able to specify the visibility depth of each entity as context for nested entities


Loaders
-------
  * the default loader is SimpleLoader
  * we can have various loaders, that can load various entities, potentially specified by kinds.
  * loaders for an entity, applicable to its kind, will be tried in order.
    * for now, loaders for the same entity and the same kind cannot be chosen among and will be tried in order until one succeeds.
      * Workaround: loader should check in the entity's configuration if any loader id is specified and compare this to an internal id.
      * since loaders are tried in order, for a specific kind the first will be 'default'
        * if no adequate kind found, ''null'' kind will be tried
      * the default loader (SimpleLoader) will only be used if no loader for that type of entity is otherwise specified.

The same policy goes for Support Infrastructures (first is default, if none specified then we use the default in NodeLoader (LocalSupport)).

The Node is always loaded by the NodeLoader.


TODO
----
   * create an alias for CategoryName.getName of only one character (e.g. s() )

Future
------
  * maybe: multiple default location for classes. E.g. for agent loaders possible classpath can be:
    * the given classpath
    * the given classpath, in the flash package
    * flash package + kind + for + KindForLoader
    * flash package + core + agent 
  * multiple support implementations
    * multiple features with the same designation
  * interfaces for common features (e.g. messaging, kb, etc)


Workings
========

Changed
-------
  * Shards can be started and stoppped regardless of agent state.


General
=======
  * make configurables more builder-like (configure return the instance?)
  * TreeObjectSet<T> would be nice -- simple keys can only be assigned to T values
    * TreeParameterSet would extend TreeObjectSet<String> 
  * implement something like addFirst / addTreeFirst ?


Other ideas
===========

lumps, lumpy agent, LUMPS as acronym










