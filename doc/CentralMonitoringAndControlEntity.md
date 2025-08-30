# CentralMonitoringAndControlEntity

The CentralMonitoringAndControlEntity class is here to supervise the behaviour of the agents in the system, and the connection between the web interface and the system. It also keeps tracks of all the nodes and agents deployed in the system.


## Interface Specifications
This class regroups all the specifications used for the creation of the web interface. When we want to update the gui from the shard controller or the monitoring shard for example, we send a message to the central monitoring with the changes wanted, then it will be parsed and added to the interface specification of the class. Finally, the updateGui method is called to make the changes in the web interface.

## Monitoring
The class receives operations from the web, and depending on the operation's information it will either send it to a target entity, or either manage it by itself.

If there is a given target entity in the received operation, the operation is parsed to send the proper command to the target entity.
When the target entity is an agent, the content of the operation received contains a specific prefix, so we know if the operation needs to be executed by the agent shard or the node.
This class define 2 prefixes to know where to send commands, depending on the received operation:

- ``CONTROL_OPERATIONS_PREFIX``, is for an operation targeting the controlShard of an agent. This kind of command will be executed by the controlShard of the agent. It must be a command an agent is able to execute by itself.
- ``NODE_OPERATIONS_PREFIX``, is for an operation targeting the parent node of an agent. This kind of command is used if we don't want to work from the agent, or if the agent isn't able to do it by itself (if the agent has been stopped for example).

If there are no target entity, the operation will be managed by this class, depending on its type:
- A status update of an agent, in which case we disable and enable this agent's button on the interface to match its status.
- A gui update, where we try to add or remove elements from the web interface by using the interface specification saved in this class to call the gui update method.
- An output from the gui.

 
