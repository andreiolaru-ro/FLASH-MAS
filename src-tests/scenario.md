# Basic scenario


* **Agents** -- there are several agents
* **Communication** -- each agent periodically sends a message to another agent given in its configuration (the *other agent*)
* **Platforms** -- agents can be deployed on other platforms
* **Interface**
	* each agent has an interface displaying a text field, a *send* button, and an area with all the received messages
	* each agent has a `quick send` button which sends a pre-set message to other agent
	* in the **reduced interface**, each agent presents only the `quick send`
* **ROS**
	* some agents, instead of sending messages to other agent, send pre-set messages to pre-set ROS nodes
	* if an interface is available, there are various buttons for different kind of messages to send (e.g. "curtains up" and "curtains down")
* **Mobility** -- each agent periodically moves / has a `move` button and moves to a pre-set or a random node
	* the control interface features a `recall` button for each agent to *call* it to the central node
* **Discovery** -- agents are able to discover all the other agents and broadcast messages
	* each agent is able to discover all the nodes
* **Agent arrays**
	* the agents are generated as an agent array, randomly distributed over the set of nodes. 


