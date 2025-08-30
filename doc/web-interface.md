# Web Interface

When the system is deployed, the user can interact with it through a web interface. This interface is available at the address ``http://localhost:8080``.\
If the interface is not available when the system is deployed, make sure the boot file you are running contains the following line:
- ``test_args += " -node main central:web";``



## Interface organisation
The web interface is a web page that allows the user to interact with the system when it is deployed. 
It is composed of a main part containing global command buttons, and a list of each of the entities deployed in the system. 
Each entity can be selected to display its own commands. At this point of development, only the agent entities' commands are fully implemented.

## Agent commands
When you select an agent, a new part of the interface appears, containing the commands available for this agent. 
The available commands depend on the shard this agent contains, and the type of boot you are running but the main commands are:
- ``Start``: Starts the agent.
- ``Kill``: Stops the agent. (The agent is stopped from the outside, it is usefull if the agent can't control itself anymore)

These commands are defined in a general command file : ``net/xqhs/flash/core/monitoring/controls.yml``

Other commands are available depending on the shards the agent contains:
- ``Stop``: Stops the agent. (The agent is stopped from the inside)

This command is defined in the agent's command file : ``net/xqhs/flash/core/control/controlBtn.yml``

- A form with a counter which increase over time: This form is used to test the monitoring shard. 
  It is used to send a message to the monitoring shard every second, and the monitoring shard will send a message back to the agent. 
  The agent will then display the message received in the form. It is also linked to a similar form in another window started at the same time of the system.
  The forms are supposed to display the same number at the same time, and update themselves if you change the value in one of them.
- A button linked to the form which send a message to the monitoring shard and display the content of the form in the terminal.

These commands are defined in the agent's command file : ``test/guiGeneration/one-port.yml``

When the state of an agent changes, the buttons of the agent are disabled or enabled to match its status. 
So when the agent is started for the first time for example, the ``Start`` button is disabled, and the ``Stop`` and ``Kill`` buttons are enabled. 
When the agent is stopped, the ``Start`` button is enabled, and the ``Stop`` and ``Kill`` buttons are disabled.
In order to see these changes on the interface, you need to refresh the page after the agent's state changed.