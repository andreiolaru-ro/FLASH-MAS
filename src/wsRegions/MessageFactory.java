package wsRegions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class MessageFactory {

    public enum MessageType {
        /**
         * Message sent from new created agent to the Region-Server from their birth region.
         * Contains the next fields: type, node (pylon name), source (sender agent name)
         */
        REGISTER,
        /**
         * Message sent from new arrived agent to the Region-Server.
         * Contains the next fields: type, node (pylon name), source (sender agent name)
         */
        CONNECT,
        /**
         * Standard message, sent from one agent to another agent.
         * Contains the next fields: type, node (pylon name), source (sender agent name), destination (receiver agent name), content (message content)
         */
        CONTENT,
        /**
         * Message sent from agent to the Region-Server, when it wants to leave to another node.
         * Contains the next fields: type, node (pylon name), source (sender agent name)
         */
        REQ_LEAVE,
        /**
         * Message sent from Region-Server to the Region-Server from the birthplace of the agent, when that agent wants to leave to another node.
         * Contains the next fields: type, node (pylon name), source (sender server name), agentName (the agent name that wants to leave)
         */
        REQ_BUFFER,
        /**
         * Message sent from Region-Server to the Region-Server from the birthplace of the agent, when the agent arrives in the new region.
         * Contains the next fields: type, node (pylon name), source (agent name), lastLocation (agent last location)
         */
        AGENT_UPDATE,
        /**
         * Message sent from Region-Server to the agent
         * Contains the next fields: type, node (pylon name), source (sender server name)
         */
        REQ_ACCEPT,
        /**
         * Message sent from a Node to another Node
         * Contains the agent in serialized form
         */
        AGENT_CONTENT,
    }

    public enum ActionType {
        /**
         * An agent receives a message from another agent.
         */
        RECEIVE_MESSAGE,
        /**
         * An agent sends a message to another agent.
         */
        SEND_MESSAGE,
        /**
         * An agent moves on another node.
         */
        MOVE_TO_ANOTHER_NODE,
        /**
         * An agent arrives on another node.
         */
        ARRIVED_ON_NODE,
    }

    /**
     * Creates the message with the given information.
     * @param node
     *            - the pylon name
     * @param agent
     *            - the sender name
     * @param type
     *            - message type
     * @param content
     *            - information extra
     * @return
     *            - returns an object with type String with the complete message
     */
    public static String createMessage(String node, String agent, MessageType type, Map<String, String> content) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Map<String, String> data = new HashMap<>();

        builder.setPrettyPrinting();
        switch (type) {
            case CONTENT:
            case REQ_BUFFER:
            case REQ_LEAVE:
            case REQ_ACCEPT:
            case AGENT_UPDATE:
            case AGENT_CONTENT:
                if (content != null) data.putAll(content);
                break;
            default:
                break;
        }
        data.put("type", type.toString());
        data.put("node", node);
        data.put("source", agent);
        return gson.toJson(data);
    }

    /**
     * Creates the logs that will be sent to the monitor entity.
     * @param actionType
     *            - action type
     * @param content
     *            - information extra
     * @return
     *            - returns an object with type String
     */
    public static String createMonitorNotification(ActionType action, String content, String time) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Map<String, String> data = new HashMap<>();

        builder.setPrettyPrinting();
        data.put("action", action.toString());
        data.put("content", content);
        //data.put("time", (new Timestamp(System.currentTimeMillis())).toString());
        data.put("time", time);
        return gson.toJson(data);
    }
}
