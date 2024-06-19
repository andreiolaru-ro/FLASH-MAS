package net.xqhs.flash.wsRegions;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.interoperability.InteroperabilityRouter;
import net.xqhs.flash.core.interoperability.InteroperableMessagingPylonProxy;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.json.AgentWaveJson;
import net.xqhs.flash.wsRegions.AgentStatus.Status;
import net.xqhs.flash.wsRegions.Constants.Dbg;
import net.xqhs.flash.wsRegions.Constants.MessageType;
import net.xqhs.util.logging.LoggerClassic;
import net.xqhs.util.logging.Unit;

/**
 * The region server in a WSRegions deployment.
 * 
 * @author Monica Pricope
 * @author Andrei Olaru
 */
public class RegionServer extends Unit implements Entity<Node> {
	/**
	 * The delay between calling the {@link #stop()} method and the server shutting down.
	 */
	protected static final int		SERVER_STOP_TIME	= 10;
	/**
	 * Unit names for servers are prefixed with this prefix.
	 */
	protected static final String	SERVER_LOG_PREFIX	= "server@";
	
	/**
	 * The field storing the {@link #isRunning()} state of the entity.
	 */
	private boolean							running;
	/**
	 * The name of this server.
	 */
	protected String						serverName;
	/**
	 * The {@link WebSocketServer}.
	 */
	protected final WebSocketServer				webSocketServer;
	/**
	 * List of agents with their home server in this region.
	 */
	protected final Map<String, AgentStatus>	regionHomeAgents	= Collections.synchronizedMap(new HashMap<>());
	/**
	 * List of the agents that arrived in this region (have the home server in other regions).
	 */
	protected final Map<String, AgentStatus>	guestAgents			= new HashMap<>();
	/**
	 * Connections with others servers.
	 */
	protected final Map<String, WSClient>		homeServers			= Collections.synchronizedMap(new HashMap<>());
	/**
	 * Keep track of all bridge entities and the platform they can route to
	 * within the pylon.
	 */
	private InteroperabilityRouter<String>		interoperabilityRouter	= new InteroperabilityRouter<>();
	/**
	 * The thread used to establish connections to other region servers.
	 */
	protected Thread							starterThread;
	
	/**
	 * @param serverPort
	 *            - the port to listen on.
	 * @param servers
	 *            - other region servers to connect to.
	 * @param server_name
	 *            - the name of this server.
	 */
	public RegionServer(int serverPort, ArrayList<String> servers, String server_name) {
		setUnitName(SERVER_LOG_PREFIX + server_name);
		setLoggerType(PlatformUtils.platformLogType());
		serverName = server_name;
		webSocketServer = new WebSocketServer(new InetSocketAddress(serverPort)) {
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
				li("<WSServer> New client connected []", webSocket);
			}
			
			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b) {
				lw("<WSServer> Connection to [] closed with code [].", webSocket, Integer.valueOf(i));
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
		
		starterThread = new Thread() {
			@Override
			public void run() {
				for(String otherServerName : servers)
					if(!homeServers.containsKey(otherServerName))
						try {
							URI serverURI = new URI("ws://" + otherServerName);
							WSClient client = new WSClient(serverURI, 10, 10000, getLogger()) {
								@Override
								public void onMessage(String s) {
									JsonObject json = JsonParser.parseString(s).getAsJsonObject();
									if(json == null)
										return;
									li("Message from server []", json.get(AgentWave.SOURCE_ELEMENT));
								}
							};
							homeServers.put(otherServerName, client);
						} catch(URISyntaxException e) {
							e.printStackTrace();
						}
			}
		};
		starterThread.start();
	}
	
	/**
	 * Process messages and redirects them to the correct destination.
	 * 
	 * @param message
	 * @param webSocket
	 */
	protected void processMessage(String message, WebSocket webSocket) {
		JsonObject json = JsonParser.parseString(message).getAsJsonObject();
		dbg(Dbg.DEBUG_WSREGIONS, "received message: ", json);
		String destination = json.has(AgentWave.COMPLETE_DESTINATION)
				? json.get(AgentWave.COMPLETE_DESTINATION).getAsString()
				: null;
		if(destination == null || destination.contains(Constants.PROTOCOL)) {
			String type = json.get(Constants.EVENT_TYPE_KEY).getAsString();
			switch(MessageType.valueOf(type)) {
			case REGISTER:
				registerMessageHandler(json, webSocket);
				break;
			case UNREGISTER:
				unregisterMessageHandler(json, webSocket);
				break;
			case CONNECT:
				connectMessageHandler(json, webSocket);
				break;
			case REQ_LEAVE:
				reqLeaveMessageHandler(json);
				break;
			case REQ_BUFFER:
				reqBufferMessageHandler(json);
				break;
			case REQ_ACCEPT:
				reqAcceptMessageHandler(json);
				break;
			case AGENT_UPDATE:
				agentUpdateMessageHandler(json);
				break;
			default:
				le("Unknown type [] in message: ", type, json);
			}
		}
		else
			contentMessageHandler(json, message);
	}
	
	@Override
	public boolean start() {
		webSocketServer.start();
		running = true;
		return true;
	}
	
	@Override
	public boolean stop() {
		try {
			webSocketServer.stop(SERVER_STOP_TIME);
			starterThread.join();
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
		return serverName;
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<?> asContext() {
		return null;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<?> context) {
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<?> context) {
		return false;
	}
	
	/**
	 * Prints the status of the server to the log.
	 */
	protected void printStatus() {
		lf("region agents:[] guest agents:[] known servers: []", regionHomeAgents, guestAgents, homeServers.keySet());
	}
	
	@Override
	protected void li(String message, Object... arguments) {
		super.li(message, arguments);
	}
	
	@Override
	protected void lf(String message, Object... arguments) {
		super.lf(message, arguments);
	}
	
	@Override
	protected void lw(String message, Object... arguments) {
		super.lw(message, arguments);
	}
	
	@Override
	protected void le(String message, Object... arguments) {
		super.le(message, arguments);
	}
	
	@Override
	protected LoggerClassic getLogger() {
		return super.getLogger();
	}
	
	/**
	 * A method that checks if the connection is still open before sending a message. Uses
	 * {@link #sendMessage(WebSocket, String, String)}.
	 * 
	 * @param webSocket
	 *            - the connection with the destination.
	 * @param message
	 *            - the message that will be sent.
	 */
	protected void sendMessage(WebSocket webSocket, AgentWaveJson message) {
		if(message.getSourceElements().length == 0)
			message.addSourceElements(getName());
		sendMessage(webSocket, message.getFirstDestinationElement(), message.getJson().toString());
	}
	
	/**
	 * Sends a message through one of the WebSocket connections.
	 * 
	 * @param webSocket
	 *            - the connection with the destination.
	 * @param destination
	 *            - the destination, used only for error displaying.
	 * @param rawMessage
	 *            - the message that will be sent, as a {@link String}.
	 */
	protected void sendMessage(WebSocket webSocket, String destination, String rawMessage) {
		if(webSocket.isOpen())
			webSocket.send(rawMessage);
		else
			le("Connection closed with entity [], unable to send message: ", destination, rawMessage);
	}
	
	/**
	 * Extracts the source of a message.
	 * 
	 * @param json
	 *            - the message.
	 * @return the source entity
	 */
	protected String extractSource(JsonObject json) {
		try {
			return json.get(AgentWave.SOURCE_ELEMENT).getAsJsonArray().get(0).getAsString();
		} catch(Exception e) {
			le("Unable to extract source from []: []", json, e);
			return null;
		}
	}
	
	/**
	 * Extracts the home region in an endpoint.
	 * 
	 * @param endpoint
	 * @return the home region
	 */
	protected String extractHomeRegion(String endpoint) {
		try {
			return endpoint.split("://")[1].split(":")[0];
		} catch(Exception e) {
			le("Unable to parse endpoint for home region []", endpoint);
			return null;
		}
	}
	
	/**
	 * Handles the case when a new agent registers in this region as its home region.
	 * 
	 * @param msg
	 *            - the message received.
	 * @param webSocket
	 *            - the connection on which the message was received.
	 */
	protected void registerMessageHandler(JsonObject msg, WebSocket webSocket) {
		String entity = extractSource(msg);
		if (msg.get(InteroperableMessagingPylonProxy.MESSAGE_BRIDGE_KEY) != null) {
			String platformPrefix = msg.get(InteroperableMessagingPylonProxy.MESSAGE_BRIDGE_KEY).getAsString();
			interoperabilityRouter.addRoutingDestinationForPlatform(platformPrefix, entity);

			// register bridge for all regions
			for (Entry<String, WSClient> homeServer : homeServers.entrySet())
				sendMessage(homeServer.getValue().client, (AgentWaveJson) new AgentWaveJson().addSourceElements(entity, Constants.PROTOCOL)
						.add(Constants.EVENT_TYPE_KEY, Constants.MessageType.REGISTER.toString())
						.add(InteroperableMessagingPylonProxy.MESSAGE_BRIDGE_KEY, platformPrefix)
						.add(InteroperableMessagingPylonProxy.IS_REMOTE, "true"));

			if (regionHomeAgents.containsKey(entity) || msg.get(InteroperableMessagingPylonProxy.IS_REMOTE) != null)
				return;
		}

		lf("Received REGISTER message from new agent ", entity);
		if(regionHomeAgents.put(entity,
				new AgentStatus(entity, webSocket, AgentStatus.Status.HOME, getName())) != null)
			le("An agent with the name [] already existed!", entity);
		printStatus();
	}
	
	/**
	 * Handles the case when a new agent unregisters.
	 * 
	 * @param msg
	 *            - the message received.
	 * @param webSocket
	 *            - the connection on which the message was received.
	 */
	private void unregisterMessageHandler(JsonObject msg, WebSocket webSocket) {
		String entity = extractSource(msg);
		lf("Received UNREGISTER message from bridge ", entity);
		AgentStatus ag = regionHomeAgents.get(entity);
		if (ag != null) {
			regionHomeAgents.remove(entity);
			if (interoperabilityRouter.removeBridge(entity)) {
				for (Entry<String, WSClient> homeServer : homeServers.entrySet())
					sendMessage(homeServer.getValue().client, (AgentWaveJson) new AgentWaveJson().addSourceElements(entity, Constants.PROTOCOL)
							.add(Constants.EVENT_TYPE_KEY, Constants.MessageType.UNREGISTER.toString()));
				le("Unregistered local bridge [].", entity);
			}
		} else if (guestAgents.remove(entity) != null) {
			String homeServer = extractHomeRegion(entity);
			if (homeServers.containsKey(homeServer))
				sendMessage(homeServers.get(homeServer).client,
						(AgentWaveJson) new AgentWaveJson().addSourceElements(entity, Constants.PROTOCOL)
								.add(Constants.EVENT_TYPE_KEY, MessageType.UNREGISTER.toString()));
			else
				le("Region server [] not connected; known servers: ", homeServer, homeServers.keySet());
		} else if (interoperabilityRouter.removeBridge(entity)) {
			le("Unregistered remote bridge [].", entity);
		} else
			le("Unable to identify agent [].", entity);

		printStatus();
	}
	
	/**
	 * Handles the case when a mobile agent registers in this region when arriving from another region.
	 * 
	 * @param msg
	 *            - the message received.
	 * @param webSocket
	 *            - the connection on which the message was received.
	 */
	protected void connectMessageHandler(JsonObject msg, WebSocket webSocket) {
		String entity = extractSource(msg);
		lf("Received CONNECT message from mobile agent ", entity);
		if(!regionHomeAgents.containsKey(entity)) {
			if(!guestAgents.containsKey(entity)) {
				guestAgents.put(entity, new AgentStatus(entity, webSocket, AgentStatus.Status.REMOTE, getName()));
				String homeServer = extractHomeRegion(entity);
				if(homeServers.containsKey(homeServer)) {
					sendMessage(homeServers.get(homeServer).client,
							(AgentWaveJson) new AgentWaveJson().add(AgentWave.CONTENT, entity)
									.add(Constants.EVENT_TYPE_KEY, MessageType.AGENT_UPDATE.toString()));
				}
				else
					le("Agent [] arrived but was not able to identify its home server []", entity, homeServer);
			}
		}
		else {
			lf("Agent [] is still in home region.", entity);
			AgentStatus ag = regionHomeAgents.get(entity);
			if(ag.getStatus() == AgentStatus.Status.OFFLINE) {
				ag.setClientConnection(webSocket);
				ag.setLastLocation(getName());
				ag.setStatus(AgentStatus.Status.HOME);
				while(ag.getStatus() == Status.HOME && !ag.getMessages().isEmpty()) {
					String saved = ag.getMessages().pop();
					li("Sending to online agent [] saved message []", entity, saved);
					sendMessage(ag.getClientConnection(), entity, saved);
				}
			}
		}
		printStatus();
	}
	
	/**
	 * Handles the case in which an agent in this region requests to leave its current location.
	 * 
	 * @param msg
	 *            - the message.
	 */
	protected void reqLeaveMessageHandler(JsonObject msg) {
		String entity = extractSource(msg);
		AgentStatus ag = regionHomeAgents.get(entity);
		lf("Request to leave from agent [] -> ", entity, ag != null ? "will accept" : "will relay");
		if(ag != null) {
			ag.setStatus(AgentStatus.Status.OFFLINE);
			AgentWaveJson wave = (AgentWaveJson) new AgentWaveJson().appendDestination(entity, Constants.PROTOCOL)
					.add(Constants.EVENT_TYPE_KEY, MessageType.REQ_ACCEPT.toString());
			for(Iterator<JsonElement> it = msg.get(AgentWave.SOURCE_ELEMENT).getAsJsonArray().iterator(); it.hasNext();)
				wave.addSourceElements(it.next().getAsString());
			sendMessage(ag.getClientConnection(), wave);
		}
		else if(guestAgents.containsKey(entity)) {
			String homeServer = extractHomeRegion(entity);
			if(homeServers.containsKey(homeServer))
				sendMessage(homeServers.get(homeServer).client,
						(AgentWaveJson) new AgentWaveJson().add(AgentWave.CONTENT, entity).add(Constants.EVENT_TYPE_KEY,
								MessageType.REQ_BUFFER.toString()));
			else
				le("Region server [] not connected; known servers: ", homeServer, homeServers.keySet());
		}
		else
			le("unable to identify agent [].", entity);
	}
	
	/**
	 * Handles the case in which an agent with its home in this region, currently remote, needs to leave its current
	 * location.
	 * 
	 * @param msg
	 *            - the message.
	 */
	protected void reqBufferMessageHandler(JsonObject msg) {
		String entity = msg.get(AgentWave.CONTENT).getAsString(); // the entity that will move.
		lf("Request to buffer for agent []", entity);
		printStatus();
		AgentStatus ag = regionHomeAgents.get(entity);
		if(ag != null) {
			ag.setStatus(AgentStatus.Status.OFFLINE);
			sendMessage(homeServers.get(ag.getLastLocation()).client, (AgentWaveJson) new AgentWaveJson()
					.add(AgentWave.CONTENT, entity).add(Constants.EVENT_TYPE_KEY, MessageType.REQ_ACCEPT.toString()));
		}
		else
			le("Agent [] is not originary from this region.", entity);
	}
	
	/**
	 * Handles an acceptance of a request to move, as received from the home region of the entity.
	 * 
	 * @param msg
	 *            - the message.
	 */
	protected void reqAcceptMessageHandler(JsonObject msg) {
		String entity = msg.get(AgentWave.CONTENT).getAsString(); // the entity that will move.
		lf("Accept request received from agent []", entity);
		AgentStatus ag = guestAgents.get(entity);
		if(ag != null) {
			sendMessage(ag.getClientConnection(),
					(AgentWaveJson) new AgentWaveJson().appendDestination(entity, Constants.PROTOCOL)
							.add(Constants.EVENT_TYPE_KEY, MessageType.REQ_ACCEPT.toString()));
			guestAgents.remove(entity);
		}
		else
			le("Agent [] is currently in this region.", entity);
	}
	
	/**
	 * Handles a location update related to an entity with its origin in this region.
	 * 
	 * @param msg
	 *            - the message.
	 */
	protected void agentUpdateMessageHandler(JsonObject msg) {
		String location = extractSource(msg);
		String entity = msg.get(AgentWave.CONTENT).getAsString();
		AgentStatus ag = regionHomeAgents.get(entity);
		if(ag != null) {
			lf("Agent [] arrived in region []. It has [] saved messages.", entity, location,
					Integer.valueOf(ag.getMessages().size()));
			ag.setLastLocation(location);
			ag.setStatus(AgentStatus.Status.REMOTE);
			if(homeServers.containsKey(location)) {
				while(ag.getStatus() == Status.REMOTE && !ag.getMessages().isEmpty()) {
					String saved = ag.getMessages().pop();
					lf("Sending to remote agent [] saved message []", entity, saved);
					sendMessage(homeServers.get(location).client, entity, saved);
				}
			}
		}
		else
			le("Agent [] (now in []) does not belong to this home server.", entity, location);
	}
	
	/**
	 * handler for normal messages between entities.
	 * 
	 * @param msg
	 *            - the message.
	 * @param message
	 *            - the raw message, as a {@link String}.
	 */
	protected void contentMessageHandler(JsonObject msg, String message) {
		String target = null;
		try {
			target = msg.get(AgentWave.DESTINATION_ELEMENT).getAsJsonArray().get(0).getAsString();
		} catch(Exception e) {
			le("Unable to determine destination of message []: ", msg, e);
			return;
		}
		dbg(Dbg.DEBUG_WSREGIONS, "Message to send from [] to [] with content ", msg.get(AgentWave.SOURCE_ELEMENT),
				target, msg.get(AgentWave.CONTENT));
		if(Dbg.DEBUG_WSREGIONS.toBool())
			printStatus();
		AgentStatus ag = regionHomeAgents.get(target);
		AgentStatus agm = guestAgents.get(target);
		if(ag != null) {
			switch(ag.getStatus()) {
			case HOME:
				dbg(Dbg.DEBUG_WSREGIONS, "Sending message directly to []", target);
				sendMessage(ag.getClientConnection(), target, message);
				break;
			case OFFLINE:
				dbg(Dbg.DEBUG_WSREGIONS, "Saved message for []", target);
				ag.addMessage(message);
				break;
			case REMOTE:
				String lastServer = ag.getLastLocation();
				dbg(Dbg.DEBUG_WSREGIONS, "Send message for [] to []", target, lastServer);
				sendMessage(homeServers.get(lastServer).client, lastServer, message);
				break;
			default:
				// can't reach here
			}
		}
		else if(agm != null) {
			dbg(Dbg.DEBUG_WSREGIONS, "Send message directly to guest agent []", target);
			sendMessage(agm.getClientConnection(), target, message);
		}
		else {
			// send to bridge
			String bridgeDestination = interoperabilityRouter.getRoutingDestination(target);
			if (bridgeDestination != null) {
				lf("Trying to send message to bridge entity [].", bridgeDestination);

				AgentStatus bridge = regionHomeAgents.get(bridgeDestination);
				if (bridge != null) {
					JsonObject modifiedMessage = InteroperabilityRouter.prependDestinationToMessage(msg, bridgeDestination);

					sendMessage(bridge.getClientConnection(), bridgeDestination, modifiedMessage.toString());
					lf("Sent message to bridge entity [].", bridgeDestination);
					return;
				}

				String regServer = null;
				try {
					regServer = extractHomeRegion(bridgeDestination);
				} catch (Exception e) {
					le("Unable to parse destination []", bridgeDestination);
					return;
				}
				dbg(Dbg.DEBUG_WSREGIONS, "Bridge [] location isn't known. Sending message to home Region Server []", bridgeDestination, regServer);
				if (homeServers.containsKey(regServer))
					sendMessage(homeServers.get(regServer).client, bridgeDestination, message);
				else
					le("Region server [] not connected; known servers: ", regServer, homeServers.keySet());

				return;
			}

			String regServer = null;
			try {
				regServer = extractHomeRegion(target);
			} catch(Exception e) {
				le("Unable to parse destination []", target);
				return;
			}
			dbg(Dbg.DEBUG_WSREGIONS, "Agent [] location isn't known. Sending message to home Region Server []", target,
					regServer);
			if(homeServers.containsKey(regServer))
				sendMessage(homeServers.get(regServer).client, target, message);
			else
				le("Region server [] not connected; known servers: ", regServer, homeServers.keySet());
		}
		
	}
}
