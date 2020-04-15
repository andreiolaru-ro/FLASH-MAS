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

public class WebSocketClientProxy extends Unit {
    {
        setUnitName("websocket-client").setLoggerType(PlatformUtils.platformLogType());
    }

    protected WebSocketClient client;

    private HashMap<String, MessageReceiver> messageReceivers	= new HashMap<>();

    void addReceiverAgent(String name, MessageReceiver receiver) {
        messageReceivers.put(name, receiver);
    }

    public WebSocketClientProxy(URI serverURI) {
        client = new WebSocketClient(serverURI) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                li("new connection to server.");
            }

            @Override
            public void onMessage(String s) {
                li("Received: []", s);

                Object obj = JSONValue.parse(s);
                if(obj == null) return;
                JSONObject jsonObject = (JSONObject) obj;

                if(jsonObject.get("destination") == null) return;
                String destination = (String) jsonObject.get("destination");
                String destAgent = destination.split(
                        AgentWave.ADDRESS_SEPARATOR)[0];
                if(!messageReceivers.containsKey(destAgent) || messageReceivers.get(destAgent) == null)
                    le("Agent [] does not exist.", destAgent);
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
