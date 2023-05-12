package httphomeserver;

import java.util.LinkedList;

public class RegionsHttpAgentStatus {
    /**
     * The agent name.
     */
    private String name;
    
    /**
     * Agent status.
     */
    private Status status;
    /**
     * Agent last location known by the birth Region-Server.
     */
    private String lastLocation;
    /**
     * Saved messages for the agents that are OFFLINE.
     */
    private final LinkedList<String> messages;

    public enum Status {
        /**
         * The agent can receive messages anytime.
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

    public RegionsHttpAgentStatus(String name, Status status, String lastLocation) {
        this.name = name;
        this.lastLocation = lastLocation;
        this.status = status;
        this.messages = new LinkedList<>();
    }

    public String getName() {
        return name;
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
		return "<" + this.name + "|" + this.status
				+ (this.name.startsWith(lastLocation) ? "" : "|" + lastLocation + "| [" + messages.size() + "] msgs")
				+ ">";
    }
}
