package websockets;

import java.net.InetSocketAddress;
import java.util.HashMap;

import net.xqhs.flash.core.Entity;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class WebSocketServerEntity implements Entity {

    private WebSocketServer webSocketServer;
    /*
     * All clients' addresses are stored in case one of them is target socket.
     */
    private HashMap<String, WebSocket> nameConnections = new HashMap<String, WebSocket>();

    public WebSocketServerEntity(int serverAddress) {
        webSocketServer = new WebSocketServer(new InetSocketAddress(serverAddress)) {

            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
             /*
              * This method sends a message to the new client.
              */
                webSocket.send("[ ServerPylon ] : Welcome to the ServerPylon!");
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                broadcast("[ ServerPylon ]" + webSocket +  " has left the room");
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
                /*
                 * The name of a new agent was send and we need to store it.
                 */
                String[] namePayload = s.split("=");
                if (namePayload.length == 2)
                    nameConnections.put(namePayload[1], webSocket);
                else {
                    /*
                    * A JSON with source, destination and content is received.
                    * */
                    Object obj = JSONValue.parse(s);
                    JSONObject jsonObject = (JSONObject) obj;

                    String destination = (String) jsonObject.get("destination");
                    WebSocket destinationWebSocket = nameConnections.get(destination);
                    destinationWebSocket.send(s);
                }
            }

            @Override
            public void onError(WebSocket webSocket, Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onStart() {
                System.out.println(("ServerPylon started successfully!"));
            }
        };
    }

    public void runServer() {
        webSocketServer.run();
    }

    public void stopServer(int time) {
        try {
            webSocketServer.stop(time);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
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
    public boolean addGeneralContext(EntityProxy context) {
        return false;
    }
}
