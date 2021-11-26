package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The host for the shadows
 *  - receives message at regular intervals from the agents
 */
public class ShadowHost extends Unit {
    {
        setUnitName("shadow-host");
        setLoggerType(PlatformUtils.platformLogType());
    }

    protected WebSocketClient client;
    /**
     * The shadow keeps a map for the agents and the first destination of those
     */
    protected Map<String, MessagingPylonProxy> agentToFirstdestination;

    protected HashMap<String, MessageReceiver> messageReceivers	= new HashMap<>();

    void addReceiverAgent(String name, MessageReceiver receiver) {
        messageReceivers.put(name, receiver);
    }

    public ShadowHost(URI serverURI) {
        client = new WebSocketClient(serverURI) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                li("new connection to server.");
            }

            @Override
            public void onMessage(String s) {
                Object obj = JSONValue.parse(s);
                if(obj == null) return;
                JSONObject message = (JSONObject) obj;

                System.out.println("Message from " + message.get("source"));
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

    public boolean send(String message) {
        System.out.println("Send from shadow");
        client.send(message);
        return true;
    }

    public boolean connectBlocking() throws InterruptedException{
        return client.connectBlocking();
    }
}
