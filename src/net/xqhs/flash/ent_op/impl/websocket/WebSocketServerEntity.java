package net.xqhs.flash.ent_op.impl.websocket;

import java.net.InetSocketAddress;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.EntityCore;

public class WebSocketServerEntity extends EntityCore {

    /**
     * The {@link org.java_websocket.server.WebSocketServer} instance.
     */
    private final WebSocketServer webSocketServer;

    /**
     * Timeout for stopping the server (sent directly to {@link WebSocketServer#stop(int)}.
     */
    private static final int SERVER_STOP_TIME = 10;

    /**
     * Map all entities to their {@link WebSocket}.
     */
    private final HashMap<String, WebSocket> entityToWebSocket = new HashMap<>();

    /**
     * Map all nodes to their {@link WebSocket}.
     */
    private final HashMap<String, WebSocket> nodeToWebSocket = new HashMap<>();

    /**
     * The name of the WebSocketServerEntity instance.
     */
    private final String webSocketServerName = "web socket server";

    /**
     * Indicates whether the implementation is currently running.
     */
    protected boolean isRunning;

    public WebSocketServerEntity(int serverPort) {
        li("Starting websocket server on port: ", serverPort);
        webSocketServer = new WebSocketServer(new InetSocketAddress(serverPort)) {
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                /*
                 * This method sends a message to the new client.
                 */
                li("New client connected []", webSocketServerName);
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                li("[] closed with exit code ", webSocketServerName, i);
            }

            /**
             * Receives message from a {@link WebSocketClient}. Messages can be:
             * <ul>
             * <li>message from one entity to another - contains a <code>destination</code> field <i>and</i> can be
             * routed.
             * <li>entity registration message. It must contain a <code>nodeName</code> field. If it registers an
             * entity, it must also contain an <code>entityName</code> field.
             * </ul>
             * The state will be printed after each registration.
             *
             * @param webSocket
             *            - the sender {@link WebSocket} client
             * @param json
             *            - the JSON string containing a message and routing information
             */
            @Override
            public void onMessage(WebSocket webSocket, String json) {
                Object obj = JSONValue.parse(json);
                if (obj == null)
                    return;
                JSONObject message = (JSONObject) obj;

                // message in transit through the server
                if (message.get("destination") != null && routeMessage(message))
                    return;

                if (message.get("nodeName") == null) {
                    lw("nodeName is null");
                }
                String nodeName = (String) message.get("nodeName");
                if (nodeName == null)
                    nodeName = "null";

                // node registration message
                if (!nodeToWebSocket.containsKey(nodeName)) {
                    nodeToWebSocket.put(nodeName, webSocket);
                    li("Registered node []. ", nodeName);
                }

                // entity registration message
                String newEntity;
                if (message.get("entityName") != null) {
                    newEntity = (String) message.get("entityName");
                    if (!entityToWebSocket.containsKey(newEntity)) {
                        entityToWebSocket.put(newEntity, webSocket);
                    }
					li("Registered entity [] on websocket [] (node []).", newEntity, webSocket, nodeName);
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

    /**
     * Tries to find a target Websocket client and sends the message to it.
     *
     * @param message - the message to be sent. Must contain a <code>destination</code> field.
     * @return - an indication of success.
     */
    private boolean routeMessage(JSONObject message) {
        String destEntity = (String) message.get("destination");

        WebSocket destinationWebSocket;
        destinationWebSocket = entityToWebSocket.get(destEntity);
        if (destinationWebSocket != null) {
            destinationWebSocket.send(message.toString());
            lf("Sent to agent: []. ", message.toString());
            return true;
        }

        destinationWebSocket = nodeToWebSocket.get(destEntity);
        if (destinationWebSocket != null) {
            destinationWebSocket.send(message.toString());
            lf("Sent to node: []. ", message.toString());
            return true;
        }

        le("Failed to find websocket for the entity [].", destEntity);
        return false;
    }

    @Override
    public boolean setup(MultiTreeMap configuration) {
        setUnitName(webSocketServerName);
        return true;
    }

    @Override
    public boolean start() {
        webSocketServer.start();
        isRunning = true;
        return true;
    }

    public boolean stop() {
        try {
            webSocketServer.stop(SERVER_STOP_TIME);
            isRunning = false;
            li("Server successfully stopped.");
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}