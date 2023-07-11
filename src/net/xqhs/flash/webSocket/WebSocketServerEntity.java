/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.webSocket;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;

/**
 * The {@link WebSocketServerEntity} class manages the routing of messages between different entities. It knows the
 * available entities in the FLASH-MAS system along with their websocket address.
 * <p>
 * Most of the functionality implemented in this class is present in the
 * {@link WebSocketServer#onMessage(WebSocket, String)} method.
 *
 * @author Florina Nastasoiu
 * @author Andrei Olaru
 */
public class WebSocketServerEntity extends Unit implements Entity<Node> {
	{
		setUnitName("websocket-server");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * Timeout for stopping the server (sent directly to {@link WebSocketServer#stop(int)}.
	 */
	private static final int				SERVER_STOP_TIME	= 10;
	/**
	 * The {@link WebSocketServer} instance.
	 */
	private WebSocketServer					webSocketServer;
	/**
	 * <code>true</code> if the server is currently running.
	 */
	private boolean							running;
	
	/**
	 * Map all entities to their {@link WebSocket}.
	 */
	private HashMap<String, WebSocket>		entityToWebSocket	= new HashMap<>();
	/**
	 * Map all nodes to their {@link WebSocket}.
	 */
	private HashMap<String, WebSocket>		nodeToWebSocket		= new HashMap<>();
	/**
	 * Keep track of all entities within a node context.
	 */
	private HashMap<String, List<String>>	nodeToEntities		= new LinkedHashMap<>();
	
	/**
	 * Creates a Websocket server instance. It must be started with {@link #start()}.
	 * 
	 * @param serverPort
	 *            - the port on which to start the server.
	 */
	public WebSocketServerEntity(int serverPort) {
		lf("Starting websocket server on port: ", Integer.valueOf(serverPort));
		webSocketServer = new WebSocketServer(new InetSocketAddress(serverPort)) {
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
				/*
				 * This method sends a message to the new client.
				 */
				li("new client connected []", webSocket);
			}
			
			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b) {
				li("[] closed with exit code ", webSocket, Integer.valueOf(i));
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
				if(obj == null)
					return;
				JSONObject message = (JSONObject) obj;
				
				// message in transit through the server
				if(message.get(WebSocketPylon.MESSAGE_DESTINATION_KEY) != null && routeMessage(message))
					return;
				
				if(message.get(WebSocketPylon.MESSAGE_NODE_KEY) == null) {
					lw("nodeName is null");
				}
				String nodeName = (String) message.get("nodeName");
				if(nodeName == null)
					nodeName = "null";
				
				// node registration message
				if(!nodeToWebSocket.containsKey(nodeName)) {
					nodeToWebSocket.put(nodeName, webSocket);
					nodeToEntities.put(nodeName, new ArrayList<>());
					lf("Registered node []. ", nodeName);
				}
				
				// entity registration message
				String entityName;
				if(message.get(WebSocketPylon.MESSAGE_ENTITY_KEY) != null) {
					// TODO: corner case when an entity is registered from the same WSPylon but a different node??
					// TODO: unregister old location?
					entityName = (String) message.get(WebSocketPylon.MESSAGE_ENTITY_KEY);
					if(message.containsKey(WebSocketPylon.UNREGISTER_KEY)) {
						if(!entityToWebSocket.containsKey(entityName))
							lw("Entity [] not registered on this server.", entityName);
						else if(entityToWebSocket.get(entityName) != webSocket)
							lw("Entity [] not registered on this server under that pylon.", entityName, webSocket);
						else {
							entityToWebSocket.remove(entityName, webSocket);
							nodeToEntities.get(nodeName).remove(entityName);
							lf("Unregistered entity [] on []. ", entityName, nodeName);
						}
					}
					else {
						if(!entityToWebSocket.containsKey(entityName)
								|| entityToWebSocket.get(entityName) != webSocket) {
							entityToWebSocket.put(entityName, webSocket);
							nodeToEntities.get(nodeName).add(entityName);
						}
						lf("Registered entity [] on []. ", entityName, nodeName);
					}
					printState();
					return;
				}
				printState();
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
	 * @param message
	 *            - the message to be sent. Must contain a <code>destination</code> field.
	 * @return - an indication of success.
	 */
	private boolean routeMessage(JSONObject message) {
		String destination = (String) message.get(WebSocketPylon.MESSAGE_DESTINATION_KEY);
		String destEntity = destination.split(AgentWave.ADDRESS_SEPARATOR)[0];
		
		WebSocket destinationWebSocket;
		destinationWebSocket = entityToWebSocket.get(destEntity);
		if(destinationWebSocket != null) {
			destinationWebSocket.send(message.toString());
			lf("Sent to agent: []. ", message.toString());
			return true;
		}
		
		destinationWebSocket = nodeToWebSocket.get(destEntity);
		if(destinationWebSocket != null) {
			destinationWebSocket.send(message.toString());
			lf("Sent to node: []. ", message.toString());
			return true;
		}
		
		le("Failed to find websocket for the entity [].", destEntity);
		return false;
	}
	
	/**
	 * Logs (fine level) the state of the server, as the lists of entities known, nodes known, and correspondence
	 * between nodes and entities.
	 */
	private void printState() {
		lf("entities: [] ; nodes: [] ; node-entities: ", entityToWebSocket.keySet(), nodeToEntities.keySet(),
				nodeToEntities);
	}
	
	@Override
	public boolean start() {
		webSocketServer.start();
		running = true;
		return true;
	}
	
	/**
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	@Override
	public boolean stop() {
		try {
			webSocketServer.stop(SERVER_STOP_TIME);
			running = false;
			li("server successfully stopped.");
			return true;
		} catch(InterruptedException e) {
			e.printStackTrace();
			return false;
		}
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
	public boolean addContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@Override
	public <C extends Entity<Node>> EntityProxy<C> asContext() {
		return null;
	}
	
	@Override
	protected void lf(String message, Object... arguments) {
		super.lf(message, arguments);
	}
	
	@Override
	protected void li(String message, Object... arguments) {
		super.li(message, arguments);
	}
	
	@Override
	protected void lw(String message, Object... arguments) {
		super.lw(message, arguments);
	}
}
