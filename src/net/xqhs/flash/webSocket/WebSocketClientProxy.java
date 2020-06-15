package net.xqhs.flash.webSocket;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.support.MessageReceiver;

/**
 * The {@link WebSocketClientProxy} manages communication with the server.
 *
 *  @author Florina Nastasoiu
 */
public class WebSocketClientProxy extends Unit {
    {
        setUnitName("websocket-client").setLoggerType(PlatformUtils.platformLogType());
    }

    protected WebSocketClient client;

    private HashMap<String, MessageReceiver> messageReceivers	= new HashMap<>();

    void addReceiverAgent(String name, MessageReceiver receiver) {
        messageReceivers.put(name, receiver);
    }

    void removeReceiverAgent(String name) {
        messageReceivers.remove(name);
    }

    public WebSocketClientProxy(URI serverURI) {
        client = new WebSocketClient(serverURI) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                li("new connection to server.");
            }

            /**
             * Receives a message from the server. The message was previously routed to this websocket client address
             * and it is further routed to a specific agent using the {@link MessageReceiver} instance. The agent is
             * searched within the context of this support.
             *
             * @param s
             *          - the JSON string containing a message and routing information
             */
            @Override
            public void onMessage(String s) {
                Object obj = JSONValue.parse(s);
                if(obj == null) return;
                JSONObject jsonObject = (JSONObject) obj;

                if(jsonObject.get("destination") == null) return;
                String destination = (String) jsonObject.get("destination");
                String destAgent = destination.split(
                        AgentWave.ADDRESS_SEPARATOR)[0];
                if(!messageReceivers.containsKey(destAgent) || messageReceivers.get(destAgent) == null)
                    le("Entity [] does not exist.", destAgent);
                else {
                    String source = (String) jsonObject.get("source");
                    String content = (String) jsonObject.get("content");
                    messageReceivers.get(destAgent).receive(source, destination, content);
                }

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                lw("Closed with exit code " + i);
            }

            @Override
            public void onError(Exception e) {
                le(Arrays.toString(e.getStackTrace()));
            }
        };
    }

    public void send(String message)
    {
        client.send(message);
    }

    public boolean connectBlocking() throws InterruptedException {
        return client.connectBlocking();
    }

}