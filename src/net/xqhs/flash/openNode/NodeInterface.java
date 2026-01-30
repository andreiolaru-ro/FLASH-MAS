package net.xqhs.flash.openNode;

import java.rmi.RemoteException;
import java.util.Map;

import net.xqhs.flash.core.Entity;

public interface NodeInterface {
	String NODE_CLI_PARAM = "cli";
	// TODO
	// exposes RMI service

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

	void addAgent(String agentName, String shardName) throws RemoteException;

}