package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class RegionServer extends Unit implements Entity {

    private static final int		SERVER_STOP_TIME	= 10;
    private WebSocketServer			webSocketServer;
    private boolean					running;
    private String                  name                = null;
    /**
     * List of agents
     */
    Map<String, AgentStatus> agentsList = new HashMap<>();

    /**
     * Conections with others servers
     */
    Map<String, WebSocketClient> clients = new HashMap<>();

    public RegionServer(int serverPort, ArrayList<String> servers) {
        {
            setUnitName("region-server-" + serverPort);
            setLoggerType(PlatformUtils.platformLogType());
        }
        name = "localhost:" + serverPort;
        webSocketServer = new WebSocketServer(new InetSocketAddress(serverPort)) {
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                li("new client connected []", webSocket);
                for (String server : servers) {
                    var nickname = (server.split("//"))[1];
                    if (!clients.containsKey(nickname)) {
                        try {
                            WebSocketClient temp_client = null;
                            int tries = 10;
                            long space = 1000;
                            while (tries > 0) {
                                try {
                                    ServerClient(new URI(server), nickname);
                                    temp_client = clients.get(nickname);
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                    le("Couldn't connect to server!");
                                }
                                if (temp_client != null && temp_client.connectBlocking())
                                    break;
                                Thread.sleep(space);
                                tries--;
                                System.out.println("Tries:" + tries);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                /*
                 * When the connection is closed, delete the entry from map
                 */
                for (AgentStatus ag : agentsList.values()) {
                    if (webSocket.toString().equals(ag.getClientConnection().toString()) && ag.getStatus().equals("ONLINE")) {
                        ag.setStatus("OFFLINE");
                    }
                }
                //printAgentList();
                //agentsList.entrySet().removeIf(entry -> webSocket.toString().equals(entry.getValue().getClientConnection().toString()));
                li(("[] closed with exit code " + i), webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
                processMessage(s, webSocket);
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
     * Create Websocket Clients to others servers.
     */
    public void ServerClient(URI serverURI, String nickname) {
        clients.put(nickname, new WebSocketClient(serverURI) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                li("new connection to server from server: " + serverURI.toString());
            }

            @Override
            public void onMessage(String s) {
                Object obj = JSONValue.parse(s);
                if(obj == null) return;
                JSONObject message = (JSONObject) obj;

                li("Message from " + message.get("source"));
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                lw("Closed with exit code " + i);
            }

            @Override
            public void onError(Exception e) {
                le(Arrays.toString(e.getStackTrace()));
            }
        });
    }

    /**
     * Process messages and redirect them to the correct destination.
     */
    private void processMessage (String message, WebSocket webSocket) {
        Object obj = JSONValue.parse(message);
        if(obj == null) return;
        JSONObject mesg = (JSONObject) obj;
        String str = (String) mesg.get("type");
        switch (str) {
            case "register":
                lf("Received message from new agent with shadow " + webSocket);
                String new_agent = (String) mesg.get("entityName");
                if (!agentsList.containsKey(new_agent)) {
                    agentsList.put(new_agent, new AgentStatus(new_agent, webSocket, "ONLINE", this.name));
                } else {
                    le("An agent with that name already exist!");
                }
               // printAgentList();
                break;
            case "connect":
                lf("Received message from arrived agent with shadow " + webSocket);
                String arrived_agent = (String) mesg.get("entityName");
                if (!agentsList.containsKey(arrived_agent)) {
                    agentsList.put(arrived_agent, new AgentStatus(arrived_agent, webSocket, "MOVED", this.name));
                    String homeServer = (arrived_agent.split("-"))[1];
                    if (clients.containsKey(homeServer)) {
                        JSONObject messageToServer = new JSONObject();
                        messageToServer.put("type", "agentArrived");
                        messageToServer.put("agentName", arrived_agent);
                        messageToServer.put("lastLocation", this.name);
                        clients.get(homeServer).send(messageToServer.toString());
                    }
                } else {
                    le("An agent with that name already exist!");
                    AgentStatus ag = agentsList.get(arrived_agent);
                    if (ag.getStatus().equals("TRANSITION")) {
                        ag.setStatus("ONLINE");
                        ag.setClientConnection(webSocket);

                        for (String saved : ag.getMessages()) {
                            System.out.println("Saved " + saved);
                            ag.getClientConnection().send(saved);
                        }
                    }
                }
                break;
            case "content":
                li("Message to send to " + mesg.get("destination"));
                String target = (String) mesg.get("destination");
                if (agentsList.containsKey(target)) {
                    AgentStatus ag = agentsList.get(target);
                    if (ag.getStatus().equals("ONLINE") || ag.getStatus().equals("MOVED")) {
                        ag.getClientConnection().send(message);
                    }
                    if (ag.getStatus().equals("TRANSITION")) {
                        ag.addMessage(message);
                    }
                } else {
                    le("Host unknown");
                    String regServer = (target.split("-"))[1];
                    if (clients.containsKey(regServer)) {
                        clients.get(regServer).send(message);
                    }
                }
                break;
            case "reqLeave":
                li("Request to leave from agent " + mesg.get("source"));
                String source = (String) mesg.get("source");
                if (agentsList.containsKey(source)) {
                    AgentStatus ag = agentsList.get(source);
                    ag.setStatus("TRANSITION");

                    printAgentList();
                    System.out.println("Accept request");
                    JSONObject messageToClient = new JSONObject();
                    messageToClient.put("type", "reqLeave");
                    messageToClient.put("response", "OK");
                    ag.getClientConnection().send(messageToClient.toString());
                }
                break;
            case "reqBuffer":
                System.out.println("Request to buffer");
                System.out.println("Accept request");
                break;
            case "agentArrived":
                li("Agent arrived");
                String movedAgent = (String) mesg.get("agentName");
                if (agentsList.containsKey(movedAgent)) {
                    AgentStatus ag = agentsList.get(movedAgent);
                    String new_location = (String) mesg.get("lastLocation");
                    ag.setStatus("MOVED");
                    ag.setLastLocation(new_location);
                    printAgentList();

                    if (clients.containsKey(new_location)) {
                        for (String saved : ag.getMessages()) {
                            System.out.println("Saved " + saved);
                            clients.get(new_location).send(saved);
                        }
                    }
                }
                break;
            default:
                System.out.println("Unknown type");
        }
    }

    public void printAgentList() {
        for (AgentStatus ag : agentsList.values()) {
            System.out.println(ag.toString());
        }
        System.out.println();
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
