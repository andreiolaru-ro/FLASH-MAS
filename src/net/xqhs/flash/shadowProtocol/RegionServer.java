package net.xqhs.flash.shadowProtocol;

import static net.xqhs.flash.shadowProtocol.MessageFactory.createMessage;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;

public class RegionServer extends Unit implements Entity {

    private static final int		SERVER_STOP_TIME	= 10;
    private final WebSocketServer	webSocketServer;
    private boolean					running;
    /**
     * List of agents with the birthplace in this region.
     */
    private final Map<String, AgentStatus> agentsList = new HashMap<>();
    /**
     * List of the agents that arrived in this region.
     */
    private final Map<String, AgentStatus> mobileAgents = new HashMap<>();
    /**
	 * Connections with others servers
	 */
    private final Map<String, WebSocketClient> clients = new HashMap<>();

    public RegionServer(int serverPort, ArrayList<String> servers, String server_name) {
        {
            setUnitName(server_name);
            setLoggerType(PlatformUtils.platformLogType());
        }
        webSocketServer = new WebSocketServer(new InetSocketAddress(serverPort)) {
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
				li("<WSServer> New client connected []", webSocket);
                for (String server : servers) {
                    if (!clients.containsKey(server)) {
                        try {
                            ServerClient(new URI("ws://" + server), server);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
				lw("<WSServer> Connection to [] closed with code [].", webSocket, i);
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
                processMessage(s, webSocket);
            }

            @Override
            public void onError(WebSocket webSocket, Exception e) {
				le("<WSServer> Connection to [] erred:", Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onStart() {
				li("<WSServer> Server started successfully.");
            }
        };
        webSocketServer.setReuseAddr(true);
    }

    /**
     * Create connections using Websocket Clients to others servers.
     * @param serverURI
     *              - server address
     * @param nickname
     *              - server name
     */
    public void ServerClient(URI serverURI, String nickname) {
        clients.put(nickname, createWebsocketClient(serverURI, nickname));
    }

    private WSClient createWebsocketClient(URI serverURI, String server) {
        return new WSClient(serverURI, 10, 3000, this.getLogger()) {
        	@Override
			public void onOpen(ServerHandshake serverHandshake) {
				li("<WSServer> New region-to-region connection to server: " + server);
            }
        	
        	@Override
			public void onMessage(String s) {
        		 Object obj = JSONValue.parse(s);
                 if(obj == null) return;
                 JSONObject message = (JSONObject) obj;
					li("Message from server []", message.get("source"));				
			}
		};
    }

    /**
     * Process messages and redirects them to the correct destination.
     */
    private void processMessage (String message, WebSocket webSocket) {
        Object obj = JSONValue.parse(message);
        if(obj == null) return;
        JSONObject mesg = (JSONObject) obj;
        String str = (String) mesg.get("type");
        MessageHandler handler = new MessageHandler();
        switch (MessageFactory.MessageType.valueOf(str)) {
            case REGISTER:
                handler.registerMessageHandler(mesg, webSocket);
                break;
            case CONNECT:
                handler.connectMessageHandler(mesg, webSocket);
                break;
            case CONTENT:
                handler.contentMessageHandler(mesg, message);
                break;
            case REQ_LEAVE:
                handler.reqLeaveMessageHandler(mesg);
                break;
            case REQ_BUFFER:
                handler.reqBufferMessageHandler(mesg);
                break;
            case REQ_ACCEPT:
                handler.reqAcceptMessageHandler(mesg);
                break;
            case AGENT_UPDATE:
                handler.agentUpdateMessageHandler(mesg);
                break;
            case AGENT_CONTENT:
                handler.agentContentMessageHandler(mesg, message);
                break;
            default:
                le("Unknown type");
        }
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
        return getUnitName();
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

    public void printAgentList() {
        for (AgentStatus ag : agentsList.values()) {
            li(ag.toString());
        }
        System.out.println();
    }

    /**
     * Class that handles messages according to their type.
     */
    class MessageHandler {
        /**
         * A method that checks if the connection is still open before sending a message.
         * @param webSocket - the connection with the destination
         * @param entityName - the destination name
         * @param message - the message that will be sent
         */
        public void sendMessage(WebSocket webSocket, String entityName, String message) {
            if (webSocket.isOpen()) {
                webSocket.send(message);
            } else {
                le("Connection closed with entity " + entityName);
            }
        }

        public void registerMessageHandler(JSONObject mesg, WebSocket webSocket) {
            String new_agent = (String) mesg.get("source");
            lf("Received REGISTER message from new agent " + new_agent);
            if (!agentsList.containsKey(new_agent)) {
                agentsList.put(new_agent, new AgentStatus(new_agent, webSocket, AgentStatus.Status.ONLINE, getUnitName()));
            } else {
                le("An agent with the name " + new_agent + " already exist!");
            }
        }

        public void connectMessageHandler(JSONObject mesg, WebSocket webSocket) {
            String arrived_agent = (String) mesg.get("source");
            lf("Received CONNECT message from mobile agent " + arrived_agent);
            if (!agentsList.containsKey(arrived_agent)) {
                if(!mobileAgents.containsKey(arrived_agent)) {
                    mobileAgents.put(arrived_agent, new AgentStatus(arrived_agent, webSocket, AgentStatus.Status.TRANSITION, getUnitName()));
                    String homeServer = (arrived_agent.split("-"))[1];
                    if (clients.containsKey(homeServer)) {
                        Map<String, String> data = new HashMap<>();
                        data.put("lastLocation", getUnitName());
                        sendMessage(clients.get(homeServer), homeServer, createMessage("", arrived_agent, MessageFactory.MessageType.AGENT_UPDATE, data));
                    }
                }
            } else {
                lf("Agent " + arrived_agent + " did not change regions");
                AgentStatus ag = agentsList.get(arrived_agent);
                if (ag.getStatus() == AgentStatus.Status.OFFLINE) {
                    ag.setStatus(AgentStatus.Status.ONLINE);
                    ag.setClientConnection(webSocket);
                    ag.setLastLocation(getUnitName());
                    for (String saved : ag.getMessages()) {
                        sendMessage(ag.getClientConnection(), arrived_agent, saved);
                    }
                }
            }
        }

        public void contentMessageHandler(JSONObject mesg, String message) {
            String target = (String) mesg.get("destination");
            li("Message to send from " + mesg.get("source") + " to agent " + target + " with content <<" + mesg.get("content") + ">>");
            if (agentsList.containsKey(target)) {
                AgentStatus ag = agentsList.get(target);
                if (ag.getStatus() == AgentStatus.Status.ONLINE) {
                    sendMessage(ag.getClientConnection(), target, message);
                }
                if (ag.getStatus() == AgentStatus.Status.TRANSITION) {
                    String lastServer = ag.getLastLocation();
                    li("Send message <<" + mesg.get("content") + ">> to agent " + target + " located on " + lastServer);
                    sendMessage(clients.get(lastServer), lastServer, message);
                }
                if (ag.getStatus() == AgentStatus.Status.OFFLINE) {
                    li("Saved message <<" + mesg.get("content") + ">> for " + target);
                    ag.addMessage(message);
                }
            } else {
                if (mobileAgents.containsKey(target)) {
                    AgentStatus ag = mobileAgents.get(target);
                    sendMessage(ag.getClientConnection(), target, message);
                } else {
                    String regServer = (target.split("-"))[1];
                    le("Agent " + target + " location isn't known. Sending message <<" + mesg.get("content") + ">> to birth Region-Server " + regServer);
                    if (clients.containsKey(regServer)) {
                        sendMessage(clients.get(regServer), regServer, message);
                    }
                }
            }
        }

        public void reqLeaveMessageHandler(JSONObject mesg) {
            String source = (String) mesg.get("source");
            lf("Request to leave from agent " + source);
            if (agentsList.containsKey(source)) {
                AgentStatus ag = agentsList.get(source);
                ag.setStatus(AgentStatus.Status.OFFLINE);
                sendMessage(ag.getClientConnection(), source, createMessage("", getName(), MessageFactory.MessageType.REQ_ACCEPT, null));
            } else {
                if (mobileAgents.containsKey(source)) {
                    String homeServer = (source.split("-"))[1];
                    if (clients.containsKey(homeServer)) {
                        Map<String, String> data = new HashMap<>();
                        data.put("agentName", source);
                        sendMessage(clients.get(homeServer), homeServer, createMessage("", getName(), MessageFactory.MessageType.REQ_BUFFER, data));
                    }
                }
            }
        }

        public void reqBufferMessageHandler(JSONObject mesg) {
            String agentReq = (String) mesg.get("agentName");
            lf("Request to buffer for agent " + agentReq);
            if (agentsList.containsKey(agentReq)) {
                AgentStatus ag = agentsList.get(agentReq);
                ag.setStatus(AgentStatus.Status.OFFLINE);
                Map<String, String> data = new HashMap<>();
                data.put("agentName", agentReq);
                String lastLocation = ag.getLastLocation();
                if (clients.containsKey(lastLocation)) {
                    sendMessage(clients.get(lastLocation), lastLocation, createMessage("", getName(), MessageFactory.MessageType.REQ_ACCEPT, data));
                }
            }
        }

        public void reqAcceptMessageHandler(JSONObject mesg) {
            String agentResp = (String) mesg.get("agentName");
            lf("Accept request received from agent " + agentResp);
            if (mobileAgents.containsKey(agentResp)) {
                AgentStatus ag = mobileAgents.get(agentResp);
                sendMessage(ag.getClientConnection(), agentResp, createMessage("", getName(), MessageFactory.MessageType.REQ_ACCEPT, null));
                mobileAgents.remove(agentResp);
            }
        }

        public void agentUpdateMessageHandler(JSONObject mesg) {
            String movedAgent = (String) mesg.get("source");
            String new_location = (String) mesg.get("lastLocation");
            lf("Agent " + movedAgent + " arrived in " + new_location + " region.");
            if (agentsList.containsKey(movedAgent)) {
                AgentStatus ag = agentsList.get(movedAgent);
                ag.setStatus(AgentStatus.Status.TRANSITION);
                ag.setLastLocation(new_location);
                if (clients.containsKey(new_location)) {
                    for (String saved : ag.getMessages()) {
                        li("Sending saved message: " + saved);
                        sendMessage(clients.get(new_location), new_location, saved);
                    }
                    ag.getMessages().clear();
                }
            }
        }

        public void agentContentMessageHandler(JSONObject mesg, String message) {
            String target = ((String) mesg.get("destination")).split("/")[0];
            String source = (String) mesg.get("source");
            li("Agent content to send from " + source + " to " + target);
            if (agentsList.containsKey(target)) {
                AgentStatus ag = agentsList.get(target);
                sendMessage(ag.getClientConnection(), target, message);
            } else {
                var getRegServer = target.split("-");
                String regServer = getRegServer[getRegServer.length - 1];
                le("Node " + target + " location isn't known. Sending message to birth Region-Server " + regServer);
                if (clients.containsKey(regServer)) {
                    sendMessage(clients.get(regServer), regServer, message);
                }
            }
        }
    }
}

