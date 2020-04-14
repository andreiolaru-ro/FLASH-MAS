package websockets;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.support.MessageReceiver;

public class WebSocketClientProxy extends WebSocketClient {
    /*
    * Keep all agents registered in the context of the current pylon.
    * */
    private HashMap<String, MessageReceiver> messageReceivers	= new HashMap<>();

    void addReceiverAgent(String name, MessageReceiver receiver) {
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
        System.out.println("[ WebSocketMessagingPylonProxy ] : " + message);

        Object obj = JSONValue.parse(message);
        if (obj != null) {
            JSONObject jsonObject = (JSONObject) obj;

            String destination = (String) jsonObject.get("destination");
            if(!messageReceivers.containsKey(destination))
                System.out.println("The agent does not exist.");
            else if(messageReceivers.get(destination) == null) {
                System.out.println("The message receiver does not exist.");
            } else {
                String source = (String) jsonObject.get("source");
                String content = (String) jsonObject.get("content");
                messageReceivers.get(destination).receive(source, destination, content);
            }
        }
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
