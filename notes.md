



* "Easy for beginners, powerful to experts" *


Current targets
===============

  * move endpoint management into a new AgentWave class.
  * use the MessagingShard interface to describe messaging shard capabilities.

### Configuration  
  * creating default node and support
    * remove default entities that are not useful (no non-portable, non-auto-generated entity has been added)
  * ported identifiable entities should be copied with different, unique, names
  * what to do when the agents are inside the support, but the node is supposed to load the agents (but the agent list is in deployment).
  * adding contexts
  * manage portables for CLI entries
  
### Deployment / loading
  * deploy a composite agent
    * what do about having support implementations when pre-loading shards
      * should we leave loading (and finding) shards to the composite agent, till after the agent is added all contexts? (who adds contexts?)

### Utilities
  * implement getAppropriateTree / addAppropriateTree methods


### General TODOs
  * establish a policy for when to use exceptions and when to use return values
  * TODOs
    * ConfigurableEntity#configure should throw exceptions and return the entity itself.


Basic Concepts and Philosophy
=============================

There are several targets that should make FLASH-MAS a good option for MAS developers, and these are also its main features:
  * system model -- the way the entities in the system are structured and the way in which they interact
  * deployment and configuration -- the way one specifies a deployment; the way in which this deployment is loaded
  * dynamics -- how entities in the system can be reconfigured at runtime
  * portability -- the code for most of the core of FLASH-MAS is portable
  * tools -- tools are offered to be able to monitor and control the system



System model
------------

### Entities
  * any part of the system which is persistent and has a life-cycle should be an Entity.
  * entities can be placed in the context of one-another. This organization is visible in the generic arguments of the Entity interface.


**Implementation**
  * Entities implement `Entity`
  * `ConfigurableEntity` is offered which offers a specific `configure` method; `ConfigurableEntity` implementations are expected to have zero-argument constructors.
  * Entities should be loadable through SimpleLoader (which supports both methods above), and only for special cases (where a factory is needed) should specialized loaders be used
  * Entity context
    * is all entities containing it, however
    * a special case are the entities of the type specified in the class hierarchy (the generic parameter for the Entity interface), which are its *proper context*

**Access control**
  * anyone with a reference to an entity is able to control it (start / stop / add/remove context)
  * an entity can expose itself to another entity as a context; the instance given as context may (and generally should, to avoid casting to a more powerful interface) be different than the actual entity, and it should relay calls to the actual entity.


### Pre-defined entities

Some entity types are predefined, but a developer should be able to add any number of additional entity types.

There are two types of pre-defined *virtual* entities which span the whole agent system are
  * *the deployment* -- the entirety of the FLASH-MAS deployment; its life-cycle is identical to the life-cycle of the FLASH-MAS system; there is only one deployment.
  * the support infrastructures -- collections of entities (pylons), which offer services to other entities in the system, especially services which involve communication, especially across machines;
    
    e.g. one support infrastructure may be able to offer id-based communication for all agents in a deployment.

Pre-defined *actual* entities are
  * *nodes* -- they represent the presence of FLASH-MAS on a physical machine; normally a node for each machine is sufficient, but more complex setups may have more than one node on a machine;
  * *pylons* -- they represent the presence of a support infrastructure on a node, and agents (or other entities) are able to use their services;
  * *agents* -- the autonomous, pro-active entities that are able to interact among each other using support infrastructure (and, more concretely, pylons);
  * *shards* -- entities encapsulating various functionality that may be useful o agents; the purpose of shards may be to:
      * if the agent is a *Composite Agent*, the agent is composed exclusively by a set of shards which interact by means of an event queue;
      * offer a more comfortable means for an agent to access the services offered by a pylon, when the pylon offers a *specific* implementation for a more general type of service (e.g. messaging);
        * while it is perfectly possible for an agent of arbitrary implementation to skip using a shard and access the services offered by the pylon directly, using shards may be a more uniform manner of abstracting pylon services, since many shards may be already be implemented so that they can be used in composite agents.


** Do we need entities at all? **

The only truly mandatory entities are nodes, pylons and agents, which are implicit in an agent system. But nodes can be automatically created and configured, and default pylon implementations are offered, so the developer may used them explicitly only when needed.


### Loaders

In order to support entities which need a special means of creation (e.g. implementations that require that the class is produced by a factory), the creation process is abstracted into Loaders. It is recommended that most entities can be loaded through `SimpleLoader`, which just creates a new instance of the entity.

Choosing a loader
  * the default loader is `SimpleLoader`
  * we can have various loaders, that can load various entities, potentially specified by kinds (see Deployment and Configuration).
  * loaders for an entity, applicable to its kind, will be tried in order.
    * for now, loaders for the same entity and the same kind cannot be chosen among and will be tried in order until one succeeds.
      * Workaround: loader should check in the entity's configuration if any loader id is specified and compare this to an internal id.
      * since loaders are tried in order, for a specific kind the first will be 'default'
        * if no adequate kind found, ''null'' kind will be tried
      * the default loader (SimpleLoader) will only be used if no loader for that type of entity is otherwise specified.

The same policy goes for Support Infrastructures (first is default, if none specified then we use the default in NodeLoader (LocalSupport)).

The Node is always loaded by the NodeLoader.

** Do we need loaders at all? **

Yes, for the cases where instantiating a class to create an entity is not enough.


### Concept Names translation from tATAmI to FLASH-MAS
  * (tATAmI-2 name -> FLASH-MAS name)
  * simulation (in terms of classes in the code) -> deployment
  * simulation (in terms of the process of running an experiment) -> simulation
  * visualization -> monitoring / control
  * component -> feature -> shard
  * platform -> support infrastructure + pylon
    * the support infrastructure is the system-spanning virtual entity that offers services such as mobility
    * the pylon is this entity's concrete presence on a node
  
  * ParameterSet -> MultiValueMap
  * TreeParameterSet -> MultiTreeMap

  * Support pylons (or pylons, for short)



Services / shards
===================

  * each shard uses one service (pylon)
  * one support infrastructure (hence also one pylon) may offer multiple services
  * pylons may recommend shard implementations



Configuration
=============

Deployment configuration contains:
  * node information
    * Schema & deployment XML (simple keys, first value counts)
    * general configuration settings (e.g. network configuration) for this node (tree key)
    * the load order -- what entities to load and order in which to load entities
    * package list (simple key, all values are relevant)
    * loaders (specified for entities and kinds)
    * support infrastructures
    * agents
    * shards
    * other entities
  
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
  * the configuration of a portable entity will be **copied** to all elements from the element where it has been declared down to its parent entity
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
      * if the parent is mandatory, entity A must be declared inside an entity of type P (additional entities may exist between A and P in more custom deployments)
      * if the parent must be auto-added, it is;
      * if the parent can be auto-generated (is a property of the parent), the deployment configuration will attempt to generate a parent P (this may lead to further generation of auto-generated parents)
      * if the parent cannot be auto-generated or auto-added (or no appropriate sibling exists), it is an error;
    * it is allowed to have **intermediate** entities between A and P, both in the optional and mandatory cases
    
**TODO: test cases**
  * no deployment file (or file not found)
  * no schema file
  * invalid deployment file
  * intermediate entities between predefined levels (e.g. node -> agentarray -> agent)
    * what happens to porting? and how are correct parents considered?


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


**Sequence**
  * load the configuration (done in NodeLoader)
    * create a node
    * load all entities in the configuration -- nothing is running yet, but a loaded entity can take registrations
    * all contexts are added
    * the node registers all entities which have loaded successfully and are
      * present in the load order, or
      * direct children in the deployment configuration
  * start the deployment (by starting each node on the current machine)
    * start the entities registered in the node, in the order in which they have been loaded


Future
------
  * maybe: multiple default location for classes. E.g. for agent loaders possible classpath can be:
    * the given classpath
    * the given classpath, in the flash package
    * flash package + kind + for + KindForLoader
    * flash package + core + agent 
  * multiple support implementations
    * multiple shards with the same designation
  * interfaces for common shards (e.g. messaging, kb, etc)


Workings
========

Changed
-------
  * Shards can be started and stopped regardless of agent state.


General
=======
  * make configurables more builder-like (configure return the instance?)
  * TreeObjectSet<T> would be nice -- simple keys can only be assigned to T values
    * TreeParameterSet would extend TreeObjectSet<String> 
  * implement something like addFirst / addTreeFirst ?






