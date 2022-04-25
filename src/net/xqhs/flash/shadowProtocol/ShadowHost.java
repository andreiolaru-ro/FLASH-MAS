package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URI;
import java.util.Arrays;

import static net.xqhs.flash.shadowProtocol.MessageFactory.createMonitorNotification;

public class ShadowHost extends Unit {
    /**
     * Connection to Region-Server.
     */
    protected WebSocketClient client;
    /**
     * Agent shard receiver.
     */
    protected MessageReceiver messageReceivers	= null;

    void addReceiverAgent(MessageReceiver receiver) {
        messageReceivers = receiver;
    }

    public ShadowHost(URI serverURI, String agent_name) {
        {
            setUnitName("proxy-" + agent_name);
            setLoggerType(PlatformUtils.platformLogType());
        }
        client = new WebSocketClient(serverURI) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                li("New connection to server.");
            }

            @Override
            public void onMessage(String s) {
                Object obj = JSONValue.parse(s);
                if(obj == null) return;
                JSONObject message = (JSONObject) obj;
                String str = (String) message.get("type");
                String content;

                switch (MessageFactory.MessageType.valueOf(str)) {
                    case CONTENT:
                        content = createMonitorNotification(MessageFactory.ActionType.RECEIVE_MESSAGE, (String) message.get("content"));
                        messageReceivers.receive((String) message.get("source"), (String) message.get("destination"), content);
                        li("Message from " + message.get("source") + ": " + message.get("content"));
                        break;
                    case REQ_ACCEPT:
                        li("[] Prepare to leave", getUnitName());
                        content = createMonitorNotification(MessageFactory.ActionType.MOVE_TO_ANOTHER_NODE, null);
                        messageReceivers.receive(null, null, content);
                        client.close();
                        break;
                    default:
                        le("Unknown type");
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

    public boolean send(String message) {
        client.send(message);
        return true;
    }

    public boolean connectBlocking() throws InterruptedException{
        return client.connectBlocking();
    }
}
