<!--- ---------------------------------------------

Copyright (C) 2021 Andrei Olaru.

This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.

Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
--------------------------------------------- -->

### Entities

* any part of the system which is persistent and has a life-cycle should be an *Entity*.
   * for instance, an agent is an entity, an artifact is an entity, an organization is an entity, or an agent group is an entity
   * but entities are also other (or in fact, all) elements which are persistent in the deployment: nodes, communication infrastructures, and also sub-agent entities such as behaviors
* entities can be placed in the context of one-another. This is implemented differently in the two phases: for an entity C which is (included) in the context of entity P:
   * in the *nested-entity model*:
      * entity P has a reference to entity C
      * the `class` of entity C is parametrized with the `class` of entity P
      * entity C may have a reference to a proxy to entity P
         * this is because a reference to an entity offers *control* over that entity via methods such as `start`, `stop`, `addContext`, etc
      * the method `C.addContext` is called to explicitly add P as a context of C
   * in the *entity-operation model*:
      * entities P and C both have a reference to the framework (or some toolkit which the framework offers)
      * relations are created as `in-scope-of` relations in the framework

#### Nested-entity model

**Implementation details**

* Entities implement `Entity`
* `ConfigurableEntity` is offered which offers a specific `configure` method; `ConfigurableEntity` implementations are expected to have zero-argument constructors.
* Entities should be loadable through `SimpleLoader` (which supports both methods above), and only for special cases (where a factory is needed) should specialized loaders be used
* Entity context
   * is all entities containing it, however
   * a special case are the entities of the type specified in the class hierarchy (the generic parameter for the Entity interface), which are its *proper context*
   * while the `Entity` interface specifies both `addContext` and `addGeneralContext`, it is **strongly recommended** that `addGeneralContext` *always works* for instances of the proper context (as some loaders, such as `SimpleLoader`, only use `addGeneralContext` to add context to entities.

**Life-cycle**

* an entity is instanced by a loader (see [Loaders](loading.md))

**Access control**

* anyone with a reference to an entity is able to control it (start / stop / add/remove context)
* an entity can expose itself to another entity as a context; the instance given as context may (and generally should, to avoid casting to a more powerful interface) be different than the actual entity, and it should relay calls to the actual entity.

### Pre-defined entities

Some entity types are predefined, but a developer should be able to add any number of additional entity types.

There are two types of pre-defined *virtual* entities which span the whole agent system are

* *the deployment* -- the entirety of the FLASH-MAS deployment; its life-cycle is identical to the life-cycle of the FLASH-MAS system; there is only one deployment.
* the support infrastructures -- collections of entities (pylons), which offer services to other entities in the system, especially services which involve communication, especially across machines;
   * e.g. one support infrastructure may be able to offer id-based communication for all agents in a deployment.

Pre-defined *actual* (*local*) entities are

* *nodes* -- they represent the presence of FLASH-MAS on a physical machine; normally a node for each machine is sufficient, but more complex setups may have more than one node on a machine;

* *pylons* -- they represent the presence of a support infrastructure on a node, and agents (or other entities) are able to use their services;

* *agents* -- the autonomous, pro-active entities that are able to interact among each other using support infrastructure (and, more concretely, pylons);

* *shards* -- entities encapsulating various functionality that may be useful o agents; the purpose of shards may be to:
  
   * if the agent is a **Composite Agent**, the agent is composed exclusively by a set of shards which interact by means of an event queue;
   * offer a more comfortable means for an agent to access the services offered by a pylon, when the pylon offers a **specific** implementation for a more general type of service (e.g. messaging);
   * while it is perfectly possible for an agent of arbitrary implementation to skip using a shard and access the services offered by the pylon directly, using shards may be a more uniform manner of abstracting pylon services, since many shards may be already be implemented so that they can be used in composite agents.
      * shards may help store information and processes that are specific to both the pylon, and to the agent (e.g. need to move with the agent, so should remain attached to it)

**Do we need entities at all?**

The only truly mandatory entities are nodes, pylons and agents, which are implicit in an agent system. But nodes can be automatically created and configured, and default pylon implementations are offered, so the developer may used them explicitly only when needed.

### Flexibility

Entities types and implementations are not fixed in any way. A developer can deploy any kind of entity, with any implementation. Moreover, the loading process of an entity is not fixed -- a loader can be provided that loads the entity in a specific manner. All this can be done without any changes to the source of FLASH-MAS, as both entities and loaders are dynamically loaded at runtime.
