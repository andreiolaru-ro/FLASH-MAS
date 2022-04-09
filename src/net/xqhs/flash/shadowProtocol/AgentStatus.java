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
     */
    public enum Status {
        ONLINE,
        OFFLINE,
        TRANSITION
    }

    private Status status = null;
    private String lastLocation = null;

    /**
     * message buffer
     */
    private LinkedList<String> messages;

    public AgentStatus(String name, WebSocket webSocket, Status status, String lastLocation) {
        this.clientConnection = webSocket;
        this.name = name;
        this.lastLocation = lastLocation;
        this.status = status;
        this.messages = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public WebSocket getClientConnection() {
        return clientConnection;
    }

    public Status getStatus() {
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

    public void setStatus(Status status) {
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

    public String toString() {
        return "Agent " + this.name + " with status " + this.status + " located on " + this.lastLocation;
    }
}
