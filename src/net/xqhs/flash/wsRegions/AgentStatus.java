package net.xqhs.flash.wsRegions;

import java.util.LinkedList;

import org.java_websocket.WebSocket;

/**
 * A structure containing information on each agent currently or originally in this region.
 */
public class AgentStatus {
	/**
	 * The agent name.
	 */
	private String						name;
	/**
	 * The Websocket object, used for sending messages to the agent. If the agent is not currently in this region, it
	 * should be <code>null</code>.
	 */
	private WebSocket					clientConnection;
	/**
	 * Agent status.
	 */
	private Status						status;
	/**
	 * Agent last location known by its home Region-Server.
	 */
	private String						lastLocation;
	/**
	 * Saved messages for the agents that are OFFLINE.
	 */
	private final LinkedList<String>	messages;
	
	/**
	 * Possible states of agents.
	 */
	public enum Status {
		/**
		 * The agent is in its home region.
		 */
		HOME,
		/**
		 * The agent is disconnected and the messages will be saved for it.
		 */
		OFFLINE,
		/**
		 * The agent is moved on another region, but it is ONLINE and can receive messages.
		 */
		REMOTE,
	}
	
	/**
	 * @param name
	 *            - the name of the agent.
	 * @param webSocket
	 *            - the client for the node where the agent is currently located.
	 * @param status
	 *            - the {@link Status} of the agent.
	 * @param lastLocation
	 *            - the last known location of the agent.
	 */
	public AgentStatus(String name, WebSocket webSocket, Status status, String lastLocation) {
		this.clientConnection = webSocket;
		this.name = name;
		this.lastLocation = lastLocation;
		this.status = status;
		this.messages = new LinkedList<>();
	}
	
	/**
	 * @return the name of the agent.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the connection where this agent is present (if in this region).
	 */
	public WebSocket getClientConnection() {
		return clientConnection;
	}
	
	/**
	 * @return the status of the agent.
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * @return the last known location (name of the region) of the agent.
	 */
	public String getLastLocation() {
		return lastLocation;
	}
	
	/**
	 * @param name
	 *            the name of the agent.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @param clientConnection
	 *            the connection where this agent is present (if in this region).
	 */
	public void setClientConnection(WebSocket clientConnection) {
		this.clientConnection = clientConnection;
	}
	
	/**
	 * @param status
	 *            the status of the agent.
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * @param lastLocation
	 *            the last known location (name of the region) of the agent.
	 */
	public void setLastLocation(String lastLocation) {
		this.lastLocation = lastLocation;
	}
	
	/**
	 * Adds a message to the queue of messages for this agent.
	 * 
	 * @param message
	 *            the message
	 */
	public void addMessage(String message) {
		this.messages.add(message);
	}
	
	/**
	 * @return the messages buffered for this agent.
	 */
	public LinkedList<String> getMessages() {
		return messages;
	}
	
	@Override
	public String toString() {
		return "<" + this.name + "|" + this.status
				+ (this.name.startsWith(lastLocation) ? "" : "|" + lastLocation + "| [" + messages.size() + "] msgs")
				+ ">";
	}
}
