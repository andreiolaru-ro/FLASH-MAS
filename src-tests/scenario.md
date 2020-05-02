# Basic scenario


* **Agents** -- there are several agents
* **Communication** -- each agent periodically sends a message to another agent given in its configuration (the *other agent*)
* **Platforms** -- agents can be deployed on other platforms

    * all features are available on all platforms

* **Interface**

    * each agent has an interface displaying two text fields (`content` and `target`), a `send` button, and an area with all the received messages
      * the `target` field is pre-filled with a favorite agent which is given in the deployment
    * each agent has a `quick send` button which sends a pre-set message to the favorite agent
    * in the *reduced interface*, each agent presents only the `quick send`
    
* **Monitoring & Control**

    * on a node featuring a control and monitoring entity, it is possible to
      * visualize all entities in the system (nodes, pylons, agents, and other, maybe not sub-agent entities)
      * execute operations on the entities (start, stop, operations in the *reduced interface*) 

* **ROS**

    * some agents, instead of sending messages to other agent, send pre-set messages to pre-set ROS nodes
    * if an interface is available, there are various buttons for different kind of messages to send (e.g. "curtains up" and "curtains down")
    
* **Mobility** -- agents are able to move between any two nodes

    * after moving, appropriate messaging, interface, and other platform- and node-specific shards will be loaded
    * [if no interface] agents periodically move
    * [if interface] each agent has a `move` button and moves to a pre-set or [if discovery] a random node
    * [if control] the interface of the control entity features a `recall` button for each agent to *call* it to the node where the control entity lives

* **Discovery** -- agents are able to discover all the other agents and broadcast messages

    * for messages where a target agent is not specified, a target agent will be picked randomly
    * each agent is able to discover all the nodes

* **Agent arrays**

    * the agents are generated as an agent array, randomly distributed over the set of nodes. 


