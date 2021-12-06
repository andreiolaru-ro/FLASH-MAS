The principle in FLASH-MAS is to allow complex deployments if needed, but also to make simple deployments very easy to specify.

While it is possible to instantiate entities and add their relations directly, in code, the *preferred* manner is to use a *deployment configuration* which contains all the necessary information for the entities to be created at startup on a particular node (and, potentially, not only).

From an abstract perspective, the deployment configuration is a tree of nested entities and other items, with the root in the *deployment*.

A lot of effort has been invested to ease the task of deploying a MAS using FLASH-MAS.

### Configuration

The deployment configuration can be specified via a scenario XML file and/or via the CLI arguments. The information is gathered in a `DeploymentConfiguration` instance, which is a `MultiTreeMap` (see the class in the project). The information in the deployment configuration is organized on two main nodes:

* in the `deployment` node, the hierarchical structure of the entities is retained
* in the `#local-id` node, all the entities in the configuration are listed, by their locally-assigned identifier.

Important takeouts:

* all deployment information can be provided both via the XML and via CLI arguments.

* information from the XML is loaded first, and information from the CLI is loaded second, so it can overwrite/refine information from the XML.

* all entities are assigned a ***local identifier*** in order to more easily read the output.

* the sequence of processing is:
  
   * an initial tree is created in the `DeploymentConfiguration`constructor. It contains
     
      * the default schema for the XML
      * the default load order
      * an unnamed node
      * a local communication pylon
  
   * the names of the XML file and of the XML schema files are searched for
     
      * if the first argument does not start with `-` then it is considered as the name of the deployment XML file and the default schema is used
     
      * otherwise, the arguments are searched for something like
        
        `-deployment_file <file-name>`
        
        and
        
        `-schema <file-name>`
        
        for the deployment file and the schema
  
   * the XML file is loaded into the configuration tree, if any
  
   * the CLI arguments are parsed and loaded into the configuration tree
  
   * additional transformations on the tree are performed (see below)

In principle, both the XML and the CLI arguments describe a tree-like structure of items. However, there are three issues:

1. CLI arguments are naturally a linear structure
2. Some other issues arise which needs automated transformations on the tree
3. Some help is given to the deployer regarding locating the classes for the entities to be loaded.

#### Structure and pre-defined items

All entities are described via key-value pairs (called *parameters*). Some entities have some predefined keys.

The pre-defined items are defined in `CategoryName`. In principle, they are 

* the pre-defined entities (deployment, node, pylon, agent, shard)
* settings:
   * the `deployment_file`
   * the `schema`
   * the list of available `packages` (see [loading](loading.md))
   * the load order (see [loading](loading.md))
   * available loaders (see [loading](loading.md))

#### XML

For the XML deployment file, the schema is described in `src-schema/deployment-schema.xsd`. The most important XML node types are `parametric` and `entitybase`, which serve as base for all other XML node types.

#### CLI

The CLI argument parser uses the foloowing grammar:

```bnf
CLI arguments 
        ::= scenario-file? category-description*

category-description     
        ::= -category(:category-property)* element-name element_description
        | -category(:category-property)* (val)*

category-property        ::= [^: ]+            [no whitespace or column]
element_description      ::= (par:val | par)* category-description*
element                 [basically anything, but interpreted as:]
        ::= part1:part2        [depending on category, can mean loader:id or type:id or category:type]
        | type:                [an unnamed element with the specified type/loader]
		| name                 [depending on category, a named element of the default type or an unnamed element with this type]
			[the exact variant is decided in Boot/NodeLoader, not in the CLI parser]

category/par/val/part1/part2/type/name     ::= [^: ]     [no whitespace or column]
```

Examples:

```bash
quick.Boot -agent myAgent mission:compute
quick.Boot -node 1 -agent myAgent -agent otherAgent -agent thirdAgent
quick.Boot -node 1 -agent myAgent -agent otherAgent -node X -agent thirdAgent
```

TODO: can we have -node -agent myAgent ??

Remarks:

* All `name:val` pairs belong in an element.
* All elements belong in a category; all categories belong in elements or at the root level.
* Any element name is preceded by its category

The root level is the local node, which may not be specifically identified. Lacking a specific "-node name" element introduction, a name for it will be automatically generated

* this way, remote nodes may be specified as well.
* the first node is the local node

There are two types of categories: simple values and not-simple-values. This cannot be distinguished in the grammar, but

* predefined categories are defined as being values or not
* not-predefined categories need to state their properties anyway, hence they will also state their property of being a simple value

*Category properties* are meta-properties which describe dynamically properties like the ones in CategoryName, for categories which are not pre-defined

* this should be allowed only for categories that are not predefined (maybe?)

* only properties that exist in CategoryName will be allowed





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
   * however, specifying visibility in the enum bay be useless because loaders/entities can still pass any context to their children

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
