<!-- ---------------------------------------------
Copyright (C) 2018 Andrei Olaru.

This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.

Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
--------------------------------------------- -->


"Easy to beginners, powerful to experts"

**Current target:**
  * [how to] deploy multiple nodes / remote nodes
  * deploy a composite agent
    * what do about having support implementations when pre-loading features
      * should we leave loading (and finding) features to the composite agent, till after the agent is added all contexts? (who adds contexts?)
  * rename TreeParameterSet and ParameterSet
  * manage portables for CLI entries



Concept Names
=============

  * simulation -> deployment
  * visualization -> monitoring / control
  * component -> feature
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


XML
---

Each SettingsName contains information on how to assemble elements from the XML into a name, if necessary. E.g. support kind and id can be assembled into kind:id.

There is a root category, namely NODE. If no node is specified, when the first entity that needs a NODE ancestor appears, an 'implicit', nameless node will be created for all entities at the root level in the XML, which are not nodes.


TODO
----
  * check duplicate names
  * TODO TODO TODO solve XML / CLI loading
  	* auto-root category could be solved by making portables from deployment (as root category) to node?
  	* solve code duplication in readXML/CLI arguments
  	* more tests with auto root category generation (both when in XML and when in CLI), plus portables, plsu not directly root-category-parented categories.
  * is feature pre-load necessary for anything?
    * in tATAmI no pre-load returns false (except for Parametric)
  * protect agents from intrusive / malicious features?
  
  * multiple components per type
    * rename ParameterSet to MultiMap or ListMap, TreeParameterSet to MultiTreeMap
  
  * establish if pre-loading an agent is optional and, if it is, make the call from load

**Future**
  * introduce entities with required name / required kind
  * should en empty no-attributes agent tag be admissible? In what scenario?



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

													
**Future:**
  * introduce tree control: "<categ" goes back to categ and << goes to root (above nodes) //>
  * between a category and its mandatory parent there may also be other categories (use case: agent arrays as entities that can be managed as a whole)
  * fuse trees from XML and CLI in the case of elements with optional parents
  * implement _q_ attribute in XML that is parsed by the CLI parser
  * implement special categories such as
    * -all-of:always category par:val+ (copy/fuse the subordinate tree to all children of category)
    * -all-of:missing category par:val+ (insert the values in the subordinate tree in all children of category, but only if no such parameter is already defined).


Fusion
------

Cases where one of the sources contains only a partially specified name are managed by the specific loader or entity that uses that configuration.


Future
------
  * -i for interactive console which allows adding (and removing) support / agents / other entities
  * -select to select the local node configuration in the deployment file (e.g. have the same deployment file for multiple nodes and just select different local nodes)



Loading / Boot
==============

Deployment configuration contains:
  * node information
    * Schema & deployment XML (simple keys, first value counts)
    * general configuration settings (e.g. network configs) for this node (tree key)
    * the load order -- what entitis to load and order in which to load entities
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
  * Entity context can only be the one specified in the class/interface; any necessary links to higher context should be managed in each specific case (as in the link between agent features and pylons)


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
  * add nodes to auto-loaded entities / manage the loading of multiple nodes
  * add a setting for categories if they should only have one instance or not (such as for text-value categories which are currently overwritten by default)

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


General
=======
  * make configurables more builder-like (configure return the instance?)
  * TreeObjectSet<T> would be nice -- simple keys can only be assigned to T values
    * TreeParameterSet would extend TreeObjectSet<String> 
  * implement something like addFirst / addTreeFirst ?


Other ideas
===========

lumps, lumpy agent, LUMPS as acronym










