package monitoringAndControl.websockets;

import net.xqhs.flash.core.support.MessageReceiver;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class WebSocketClientProxy extends WebSocketClient {
    /*
     * Keep all agents registered in the context of the current pylon.
     * */
    private HashMap<String, MessageReceiver> messageReceivers	= new HashMap<>();

    public void addReceiverAgent(String name, MessageReceiver receiver) {
        messageReceivers.put(name, receiver);
    }

    MessageReceiver getMessageReceiver(String name) {
        return messageReceivers.get(name);
    }

    public WebSocketClientProxy(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public WebSocketClientProxy(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("[ WebSocketMessagingPylonProxy ] " + "new connection opened " + this.isOpen());
    }

    @Override
    public void onMessage(String message) {
        Object obj = JSONValue.parse(message);
        if(obj == null) return;
        JSONObject jsonObject = (JSONObject) obj;

        String agentName;
        if(jsonObject.get("simpleDest") == null) return;
        agentName = (String)jsonObject.get("simpleDest");
        if(!messageReceivers.containsKey(agentName))
        {
            System.out.println("The agent does not exist.");
            return;
        }

        String source = (String) jsonObject.get("source");
        String destination = (String) jsonObject.get("destination");
        String content = (String) jsonObject.get("content");
        messageReceivers.get(agentName).receive(source, destination, content);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.println("[ WebSocketMessagingPylonProxy ] " + " closed with exit code " + i +
                " because of " + s);
    }

    @Override
    public void onError(Exception e) {
        System.out.println(Arrays.toString(e.getStackTrace()));

    }
}

