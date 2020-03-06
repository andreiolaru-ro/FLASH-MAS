package websocket;

import net.xqhs.flash.core.support.MessageReceiver;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

public class WebSocketMessagingPylonProxy extends WebSocketClient {
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

    public WebSocketMessagingPylonProxy(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public WebSocketMessagingPylonProxy(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("[ WebSocketMessagingPylonProxy ] " + "new connection opened " + this.isOpen());
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[ WebSocketMessagingPylonProxy ] : " + message);
        String[] messagePayload = message.split("@");
        if (messagePayload.length == 3) {
            String destination = messagePayload[1];
            if(!messageReceivers.containsKey(destination))
                System.out.println("The agent does not exist.");
            else  {
                String source = messagePayload[0];
                String msg = messagePayload[2];
                messageReceivers.get(destination).receive(source, destination, msg);
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
