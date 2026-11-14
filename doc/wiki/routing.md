

**tentative**


* general
    * on a node there are several pylons, managing various protocols
    * there is an order of the pylons, the order in which they were added
    * pylons are known by a *local pylon*
    * all entities register with the local pylon
        * although some specialized entities may only register with some pylons
    * the local pylon sends messages via an **appropriate** pylon


* entity directory
    * correspondence between (short) entity names and their complete names
    * short names can be used if they are in the directory
    * if it is not in the directoy, it can be attempted to be used as a long name
    * the short name if after the last slash, or if there is any double slash, everything after the double slash, if the group of 2 or more slashes is not preceded by a colon (e.g. http://server/path//entity/with/slashes)
    * pairs can be given
      * in configuration
      * when first sending to an entity
      * when receiving from an entity

* directory servers
  * there is a number of directory servers
  * any node is linked to a directory server
    * when looking for an entity not in the local directory, the message can be sent to the directory server, which
      * will relay the message to its destination
      * will respond with the location of the destination, to be added to the local node directory
      * will remember which nodes know about which entities, in order to send them updates
  * when a new directory server gets connected
    * it is given the address of one of the already started directory servers
    * it synchronizes with that server
  * when an entity leaves from a node, the node stores [for a given time] the address of the node the entity move to
    * if a node receives a message for an entity which left that node, it redirects the message to the other location
      
Pylons can support mobility in different manners
  * no support
  * full, based on names containing the home server of the entity (like wsRegions)
  * partial
    * the infrastructure will move the agent
    * the agent must leave a proxy on its home server (shadow style), keeping the original name
    * the agent changes name according to its current server
    * the proxy relays all the messages (shadow-style, but handled by the proxy, not by the infrastructure)
    
    
**efficient routing**

in a distributed approach, regions are unavoidable as a means to divide the network and as servers that others can connect to (are routable)


_agent names without host name?_


* short path protocol modification
	* normally messages for agent go through agent's home server
	* current region server can send to message sender an AGENT_LOCATION message
	* the sending region server sends a Long_path_stop message through the long path, and a short_path_begin meessage directly to the agent's current region
	* the current region will wait for the long_path_stop message before accepting messages sent after the short_path_begin message. This happens on a per-agent/sending-region/current-region basis
	
	* when the agent is leaving, the current region sends a short_path_stop message to all regions using short paths for that agent
	* the sending region sends 
	 * a short_path_end message to the current region signaling there will be no more messages via the short path; this will be relayed to the home region
	 * a long_path_resume message to the home region signaling that any messages after this one should be sent to the agent after all the messages from the current region, ending with the _short_path_end message
	* the sending region will the route any messages for the agent via the long path









