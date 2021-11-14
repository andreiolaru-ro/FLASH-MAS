package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;

/**
 * Entity that keeps tracks of the shadows address
 *
 * Send-flow:
 *      If Agent A wants to send a message to Agent B
 *      A sends the message to HomeServer
 *      HomeServer sends the message to the shadow of B
 *      And the shadow sends the message to Agent B
 */
public class ShadowHomeServer extends Unit implements Entity {

    {
        setUnitName("shadow-home-server");
        setLoggerType(PlatformUtils.platformLogType());
    }

    private static final int		SERVER_STOP_TIME	= 10;
    private WebSocketServer			webSocketServer;
    private boolean					running;

    /**
     * Mapping every Agent to its shadow
     */
    Map<WebSocket, ArrayList<String>> shadowAndAgents;

    Map<String, ArrayList<WebSocket>> pylonAndShadows;

    public ShadowHomeServer(int serverPort) {
        webSocketServer = new WebSocketServer(new InetSocketAddress(serverPort)) {
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                /**
                 * This method sends a message to the new client.
                 */
                li("new client connected []", webSocket);
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                li(("[] closed with exit code " + i), webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
                Object obj = JSONValue.parse(s);
                if(obj == null) return;
                JSONObject message = (JSONObject) obj;

                if (message.get("nodeName") != null && message.get("entityName") != null) {
                    System.out.println("Received message from new agent with shadow " + webSocket);

                }

            }

            @Override
            public void onError(WebSocket webSocket, Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onStart() {
                li("Server started successfully.");
            }
        };
        webSocketServer.setReuseAddr(true);
    }

    @Override
    public boolean start() {
        webSocketServer.start();
        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        try
        {
            webSocketServer.stop(SERVER_STOP_TIME);
            running = false;
            return true;
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean addContext(EntityProxy context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy context) {
        return false;
    }

    @Override
    public EntityProxy asContext() {
        return null;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy context) {
        return false;
    }
}
