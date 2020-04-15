package net.xqhs.flash.core.monitoring.websockets;

import java.net.InetSocketAddress;
import java.util.*;

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
    private HashMap<String, WebSocket> nodeToWebSocket = new HashMap<>();
    private HashMap<String, WebSocket> agentToWebSocket = new HashMap<>();
    private HashMap<String, List<String>> nodeToAgents = new LinkedHashMap<>();

    private String centralNodeName                     = null;
    private WebSocket centralNodeWebSocket             = null;

    public WebSocketServerEntity(int serverAddress) {
        webSocketServer = new WebSocketServer(new InetSocketAddress(serverAddress)) {

            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                /*
                 * This method sends a message to the new client.
                 */
                webSocket.send("Welcome to the ServerPylon!");
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                broadcast("[ServerPylon]" + webSocket +  " has left the room");
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
                Object obj = JSONValue.parse(s);
                if(obj == null) return;

                JSONObject jsonObject = (JSONObject) obj;

                if(jsonObject.get("nodeName") == null) return;
                String nodeName = (String)jsonObject.get("nodeName");

                boolean isCentralNode;
                if(jsonObject.get("isCentral") != null)
                {
                    isCentralNode = (boolean)jsonObject.get("isCentral");
                    if(isCentralNode)
                    {
                        centralNodeName = nodeName;
                        centralNodeWebSocket = webSocket;
                    }
                    nodeToWebSocket.put(nodeName, webSocket);
                    nodeToAgents.put(nodeName, new ArrayList<>());

                }

                String newAgent;
                if(jsonObject.get("agentName") != null)
                {
                    newAgent = (String)jsonObject.get("agentName");
                    agentToWebSocket.put(newAgent, webSocket);
                    nodeToAgents.get(nodeName).add(newAgent);
                }

                String agentDestination;
                if(jsonObject.get("simpleDest") != null)
                {
                    agentDestination = (String)jsonObject.get("simpleDest");
                    if(agentDestination.equals(centralNodeName))
                        centralNodeWebSocket.send(s);
                    else
                    {
                        WebSocket wsDestination = agentToWebSocket.get(agentDestination);
                        wsDestination.send(s);
                    }
                }

                nodeToWebSocket.entrySet().forEach(entry->{
                    System.out.println("#nod: " + entry.getKey());
                });

                agentToWebSocket.entrySet().forEach(entry->{
                    System.out.println("#agent: " + entry.getKey());
                });

                nodeToAgents.entrySet().forEach(entry->{
                    System.out.println("#nod->ent: " + entry.getKey() + " : " + entry.getValue());
                });

                System.out.println("##CentralNode " + centralNodeName);
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
    public boolean removeGeneralContext(EntityProxy context) {
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

