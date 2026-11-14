<!--- ---------------------------------------------
Copyright (C) 2021 Andrei Olaru.

This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.

Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
--------------------------------------------- -->

In order to support flexibility, the creation of entities should be performed via *Loaders*.

**Do we need loaders at all?**

Yes, for the cases where instantiating a class to create an entity is not enough.

For instance, if we want to load a JADE agent (and we have access to a JADE platform), we cannot instantiate the agent directly, rather we need to call a method of a JADE container -- this can be achieved via a specialized loader for JADE agents.

### Loaders

In order to support entities which need a special means of creation (e.g. implementations that require that the class is produced by a factory), the creation process is abstracted into Loaders. It is recommended that most entities can be loaded through `SimpleLoader`, which just creates a new instance of the entity.

##### Choosing a loader

* the default loader is `SimpleLoader`
* we can have various loaders, that can load various entities, potentially specified by kinds (see [Deployment and Configuration](deployment.md)).
   * identifying the appropriate loader, as well as the appropriate class, can be done using the `Loader.autoFind` static method
* loaders for an entity, applicable to its kind, will be tried in order.
   * for now, loaders for the same entity and the same kind cannot be chosen among and will be tried in order until one succeeds.
      * Workaround: loader should check in the entity's configuration if any loader id is specified and compare this to an internal id.
      * since loaders are tried in order, for a specific kind the first will be 'default'
         * if no adequate kind is found, ''null'' kind will be tried
      * the default loader (SimpleLoader) will only be used if no loader for that type of entity is otherwise specified or otherwise succeeds.

The Node is always loaded by the NodeLoader, which also loads all entities specified in the initial deployment.

### Loading and Booting the system

See also [running](running.md).

Normally, a FLASH-MAS deployment is booted via the `FlashBoot` class, which loads the deployment configuration for all the local nodes and starts those nodes. Main operations related to loading and starting the system are performed by `NodeLoader` which:

* loads the deployment configuration for all nodes declared in the deployment.
* loads the specified loaders. Loaders are organized by category and kind.
   * the default loader is the `SimpleLoader` or the first loader for the `null` category or the first loader for the `null` kind in the `null` category (if any).
* for each node, loads the entities which should be started on that node, considering the ***load order*** for the categories (the order in which categories should be loaded, e.g. pylons before agents)
   * in order to compute how to load an entity, its category and kind are considered (see [deployment](deployment.md))
   * if there are loaders declared for this category, the loader for the entities kind is used
      * if the entity has no kind, the loader for the `null` kind in this category is used, and if no such loader exists, the loader for the first kind declared for the category
   * otherwise, the default loader is used
   * if loading fails with the chosen loader, the default loader is also tried.
   * when the default loader is used, an appropriate class must be found to instantiate. This is done using `Loader#autoFind` which looks for a `classpath` parameter in the entity or otherwise tries to locate a class in the packages, according to the category and kind of the entity (see its documentation).
   * when `Loader#load` is called, all the entities which form the context of the loaded entity are also transmitted.
   * loaded entities which are in the *load order* or direct children of the node in the deployment configuration are `register`ed with the node
* then, `FlashBoot` *starts* the node(s), which in turn *start* the entities which exist in their scope
