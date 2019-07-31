



* "Easy for beginners, powerful to experts" *


Current targets
===============

### Configuration  
  * sort out deployment configuration in terms of how portability and visibility are implemented
    * who adds contexts and when (after all configuration is loaded or both after XML load and CLI parse?) ?
    * how are contexts correctly fused, especially when porting items?
  * manage portables for CLI entries
  
### Deployment / loading
  * deploy a composite agent
    * what do about having support implementations when pre-loading features
      * should we leave loading (and finding) features to the composite agent, till after the agent is added all contexts? (who adds contexts?)

### Utilities
  * implement getAppropriateTree / addAppropriateTree methods



Basic Concepts and Philosophy
=============================






### Concept Names translation from tATAmI to FLASH-MAS
  * (tATAmI-2 name -> FLASH-MAS name)
  * simulation (in terms of classes in the code) -> deployment
  * simulation (in terms of the process of running an experiment) -> simulation
  * visualization -> monitoring / control
  * component -> feature -> shard
  * platform -> support infrastructure + pylon
    * the support infrastructure is the system-spanning virtual entity
    * the pylon is this entity's concrete presence on a node
  
  * ParameterSet -> MultiValueMap
  * TreeParameterSet -> MultiTreeMap

  * Support pylons (or pylons, for short)

Services / features
===================

  * each shard uses one service (pylon)
  * one support infrastructure (hence also one pylon) may offer multiple services
  * pylons may recommend shard implementations



Configuration
=============

Assembling parts to form a name would be done *only* for entities for which this union can stand for its identifier.

Unnamed entities are allowed, but cannot be referenced (such as for <in-context-of>).

There is a root category, namely DEPLOYMENT. If no NODE is specified, a default one will be created for entities that need to be inside a node (e.g. agents, etc). This implicit node will be the first node among the nodes with no name. If a node with no name has already been introduced, it will be the same one.

The local node is the first node not specifically designated as remote.

**Future**
  * translate the CategoryName enum into a set of rules (could be a MultiTreeMap?) that is further adjustable in the configuration.

Context
-------

**Visibility**
  * Can't implement in visibility in the enum because lower level entities are not yet defined;
  * Therefore, visibility can be implemented
    * in the deployment, through an attribute (TODO)
    * ad-hoc by the implementation of each loader / each entity that loads other entities
 
**Porting**
  * an entity B can be ported from an entity A to an entity C (which is also its declared parent in hierarchy)
  * the entity B must have been declared (in the deployment) as a descendant of entity A
  * the configuration of a portable entity will be copied to all elements from the element where it has been declared down to its parent entity
  * actual entity instances will be created only for the copy inside the parent (entity C)
  
**Parents**
  * parents specify where an entity can be declared (inside which other entity)
  * for an entity (of type) A with parent P:
    * the parent can be optional
      * the optional indication for parents is used to structure the entities in two ways:
        * determine whether an entity is in the correct context even if some entity types are missing from the context (TODO: add example)
        * establish some hierarchy of entities that helps navigation when parsing CLI arguments
      * if the parent can be auto-added, and a sibling instance P exists, entity A will be added automatically to instance P (the one with the highest priority)
    * the parent can be mandatory
      * if the parent is mandatory, entity A must be declared inside an entity of type P
      * if the parent can be auto-generated (is a property of the parent), the deployment configuration will attempt to generate a parent P (this may lead to further generation of auto-generated parents)
      * if the parent must be auto-added, it is; if a sibling instance does not exist, it is an error and entity A is not added to the deployment
    
**TODO: test cases**
  * no deployment file (or file not found)
  * no schema file
  * invalid deployment file


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
  * check duplicate names for identifiable entities / check identifiables
  * check if in the end unique entities are unique
  * implement all properties also as attributes in the configuration
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






