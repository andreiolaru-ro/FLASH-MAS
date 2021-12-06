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

### Booting the system

Normally, a FLASH-MAS deployment is booted via the `FlashBoot` class, which loads the deployment configuration for all the local nodes and starts those nodes. Main operations related to loading and starting the system are performed by `NodeLoader` which:

* loads the deployment configuration
* for each node, loads the entities which should be started on that node, considering the ***load order*** (the order in which entities should be loaded, e.g. pylons before agents)
* then, `FlashBoot` *starts* the node(s), which in turn *start* the entities which exist in their scope.
