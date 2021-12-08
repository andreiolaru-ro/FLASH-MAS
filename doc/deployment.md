<!--- ---------------------------------------------
Copyright (C) 2021 Andrei Olaru.

This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.

Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
--------------------------------------------- -->

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

In the deployment configuration tree, there are two keys: `deployment` and `#local-id` (see above). The `deployment` key contains keys for each *category*. Each category then contains keys for the *name* of each entity (element) in that category. Each element then contains (besides its own parameters) keys for each subordinate category, and so on.

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

The CLI argument parser uses the following grammar:

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

Remarks:

* All `name:val` pairs belong in an element.
* All elements belong in a category; all categories belong in elements or at the root level.
* Any element name is preceded by its category
* In the tree (implemented as a `MultiTreeMap`) there are two types of categories: simple values (which are strings) and not-simple-values (which are also trees). This cannot be distinguished in the grammar, but:
   * predefined categories are defined as being values or not
   * not-predefined categories need to state their properties anyway, hence they will also state their property of being a simple value
* In order to correctly identify the appropriate *loader* for an entity, it is possible to specify, apart from its category (e.g. *pylon*), also its ***kind***. This is done together with the identifier. Usually, this is specified like `-pylon local:local1`, where `local` is the kind and `local1` is the identifier; or `-agent jade:agent1`, where `jade` is the kind and `agent1` is the identifier. See more in [loading](loading.md).
   * whether this is possible for a category is specified in the appropriate `CategoryName` instance.
   * the kind can also be specified as an individual parameter of the entity, such as `kind:jade`
* The deployment configuration allows unnamed entities, but these cannot be referenced (such as for <in-context-of>).

##### Making a linear structure into a tree structure

This is done while parsing CLI arguments, by looking at the *category* of the element.

Let's start with an example: take the CLI command line:

```bash
quick.Boot -node central -node node6 -pylon local:comms -agent Facilitator service:directory
 -agent MyAgent pingPeriod:5 -pylon websocket:WS -agent main1 -agent main2 -agent main3
```

The corresponding deployment configuration looks like this (parameter values in brackets):

```
deployment
  node
    central
    node6
      pylon
        comms [kind:local]
          agent
            Facilitator [service=directory]
            MyAgent [pingPeriod=5]
        WS [kind:websocket]
          agent
            main1
            main2
            main3
```

At any time while parsing the arguments, there is a *current position* in the configuration tree where we "are". The initial position is at the level of the deployment. For instance, after the arguments `-node node6 -pylon local:comms -agent Facilitator service:directory -agent MyAgent pingPeriod:5`, the current position in the tree is:

```
deployment
  node
    node6
      pylon
        comms [kind:local]
          agent
            Facilitator [service=directory]
            MyAgent [pingPeriod=5]
              *here*
```

Considering the *current position*, the following rules are used to integrate a new element of category *Cat*:

* if there is an ancestor of the current position of category *Cat*, the new element will be placed in that category (see how the `WS` pylon is integrated in the example above).
* if the category is not predefined, or a predefined parent category is not specified in `CategoryName` (such as nodes for pylons, pylons for agents, etc), the new element (and its category) is added at the current position.
* if the category is predefined or there is supposed the new element (and its category) is added at top level, in the `deployment`.

### Context

Entities exist in the context of other entities. For instance, an agent exists in the context of a pylon, of a node, and of the deployment, but also potentially in a group, an organization, etc. We say that entities "above" are added to the context of elements "below", and elements "below" are added in the context of elements"above".

Currently, all elements in categories which are declare (in `CategoryName`) as *visible on path* are visible to all elements below and, hence, are added to the context of elements below.

**Discussion on visibility**

* It would be desired to be able to limit the visibility of a category to be no deeper than a specific other category.
* Can't implement in visibility in the enum because lower level entities are not yet defined;
* Therefore, visibility can be implemented
   * in the deployment, through an attribute (TODO)
   * ad-hoc by the implementation of each loader / each entity that loads other entities
   * however, specifying visibility in the enum may be useless because loaders/entities can still pass any context to their children

### Porting

It is desired that, for instance, if there are multiple nodes declared in the deployment, and each should use a pylon with the same settings for all nodes, that the template specification of the pylon is only written once in the deployment. SImilarly, it may be desired that (some) loaders or some packages are available in all nodes, but only specified once.

So, the use case is as follows:

* the 'template' pylon is described before any of the nodes.
* each node gets a separate actual pylon.

This is done by means of ***porting***, which is done after all of the deployment information is read from the XML and the CLI. The following rules are applied:

* an entity B can be ported from an entity A to an entity C (which is also its declared parent in hierarchy)
* the entity B must have been declared (in the deployment) as a descendant of entity A
* the configuration of a portable entity will be **copied** to all elements from the element where it has been declared down to its parent entity
* actual entity instances will be created only for the copy inside the parent (entity C), with new local identifiers.

### Parents

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
