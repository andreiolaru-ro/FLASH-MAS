package net.xqhs.flash.shadowProtocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class MessageFactory {

    public enum MessageType {
        REGISTER,
        CONNECT,
        CONTENT,
        REQ_LEAVE,
        REQ_BUFFER,
        AGENT_UPDATE,
        REQ_ACCEPT
    }

    public static String createMessage(String node, String agent, MessageType type, Map<String, String> content) {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        Gson gson = builder.create();

        Map<String, String> data = new HashMap<>();
        switch (type) {
            case REGISTER:
                data.put("type", "REGISTER");
                break;
            case CONNECT:
                data.put("type", "CONNECT");
                break;
            case CONTENT:
                data.put("type", "CONTENT");
                data.putAll(content);
                break;
            case REQ_BUFFER:
                data.put("type", "REQ_BUFFER");
                break;
            case REQ_LEAVE:
                data.put("type", "REQ_LEAVE");
                break;
            case REQ_ACCEPT:
                data.put("type", "REQ_ACCEPT");
                break;
            case AGENT_UPDATE:
                data.put("type", "AGENT_UPDATE");
                data.putAll(content);
                break;
        }

        data.put("node", node);
        data.put("source", agent);

        String JSONObject = gson.toJson(data);
        System.out.println(JSONObject);

        return JSONObject;
    }
}
