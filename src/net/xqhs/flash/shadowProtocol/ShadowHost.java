package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ShadowHost extends Unit {

    protected WebSocketClient client;

    protected MessageReceiver messageReceivers	= null;

    protected String agent_name = null;

    protected String serverName = null;

    void addReceiverAgent(String name, MessageReceiver receiver) {
        agent_name = name;
        messageReceivers = receiver;
    }

    public ShadowHost(URI serverURI, String agent_name) {
        {
            setUnitName("proxy-" + agent_name);
            setLoggerType(PlatformUtils.platformLogType());
        }
        serverName = serverURI.toString();
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
                String str = (String) message.get("type");
                switch (str) {
                    case "content":
                        messageReceivers.receive((String) message.get("source"), (String) message.get("destination"), (String) message.get("content"));
                        li("Message from " + message.get("source") + ": " + message.get("content"));
                        break;
                    case "reqLeave":
                        String response = (String) message.get("response");
                        if (response.equals("OK")) {
                            li("Prepare to leave");
                            messageReceivers.receive("", "", "stop");
                            client.close();
                        }
                        break;
                    default:
                        System.out.println("Unknown type");
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
        li("Send from shadow");
        client.send(message);
        return true;
    }

    public boolean connectBlocking() throws InterruptedException{
        return client.connectBlocking();
    }
}
