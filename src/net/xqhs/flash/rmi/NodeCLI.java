package net.xqhs.flash.rmi;

import java.util.Map;

import net.xqhs.flash.core.Entity;

public class NodeCLI {
	
	public static interface NodeInterface {
		/**
		 * @return entity name -> entity status (e.g. running, etc)
		 */
		// necessary methods to access from Node
		Map<String, String> listEntities();
		
		/**
		 * Stops an entity running on this node.
		 * 
		 * @param entityName
		 *            - the name of the entity to stop.
		 * @return the return value of {@link Entity#stop()}.
		 */
		boolean stopEntity(String entityName);
	}
	
	public static final String NODE_CLI_PARAM = "cli";
	// TODO
	// exposes RMI service
	
	public NodeCLI(NodeInterface node) {
		// TODO Auto-generated constructor stub
	}
}
