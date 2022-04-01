package net.xqhs.flash.shadowProtocol;

import org.java_websocket.WebSocket;

import java.util.LinkedList;

public class AgentStatus {
    private String name = null;
    private WebSocket clientConnection = null;
    /**
     * An agent can be in one of the following states:
     *      ONLINE => agent up
     *      OFFLINE => agent is disconnected/moving
     *      TRANSITION => agent is located on another region
     *      OUTER => status of the agent moved on different pylon on another region
     *      INSIDE => status of the agent moved on different pylon on the same region
     */
    private String status = null;
    private String lastLocation = null;
    private boolean localNode = false;
    /**
     * message buffer
     */
    private LinkedList<String> messages;

    public AgentStatus(String name, WebSocket webSocket, String status, String lastLocation, boolean localNode) {
        this.clientConnection = webSocket;
        this.name = name;
        this.lastLocation = lastLocation;
        this.status = status;
        this.messages = new LinkedList<>();
        this.localNode = localNode;
    }

    public String getName() {
        return name;
    }

    public WebSocket getClientConnection() {
        return clientConnection;
    }

    public String getStatus() {
        return status;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClientConnection(WebSocket clientConnection) {
        this.clientConnection = clientConnection;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
    }

    public void addMessage(String mes) {
        this.messages.add(mes);
    }

    public LinkedList<String> getMessages() {
        return messages;
    }

    public boolean isLocalNode() {
        return localNode;
    }

    public void setLocalNode(boolean localNode) {
        this.localNode = localNode;
    }

    public String toString() {
        return "Agent " + this.name + " with status " + this.status + " located on " + this.lastLocation + " " + isLocalNode();
    }
}
