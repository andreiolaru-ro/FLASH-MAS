

*tentative*


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
    * the short name if after the last slash, or if there is any double slash, everything after the double slash (e.g. http://server/path//entity/with/slashes
    * pairs can be given
      * in configuration
      * when first sending to an entity
      * when receiving from an entity

      
      
Pylons can support mobility in different manners
  * no support
  * full, based on names containing the home server of the entity (like wsRegions)
  * partial
    * the infrastructure will move the agent
    * the agent must leave a proxy on its home server (shadow style), keeping the original name
    * the agent changes name according to its current server
    * the proxy relays all the messages (shadow-style, but handled by the proxy, not by the infrastructure)
    