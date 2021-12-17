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
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;

/**
 *  The {@link WebSocketServerEntity} class manages the routing of messages between different entities. It knows the
 *  available agents in the FLASH-MAS system along with their websocket address. It also keeps track of central nodes.
 *
 *  @author Florina Nastasoiu
 */
public class WebSocketServerEntity extends Unit implements Entity
{
	{
		setUnitName("websocket-server");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	private static final int		SERVER_STOP_TIME	= 10;
	private WebSocketServer			webSocketServer;
	private boolean					running;

	/**
	 * Map all entities to their {@link WebSocket}.
	 */
	private HashMap<String, WebSocket> entityToWebSocket = new HashMap<>();
	/**
	 * Map all nodes to their {@link WebSocket}.
	 * */
	private HashMap<String, WebSocket> nodeToWebSocket  = new HashMap<>();
	/**
	 * Keep track of all entities within a node context.
	 * */
	private HashMap<String, List<String>> nodeToEntities = new LinkedHashMap<>();

	/**
	 * @param message
	 * 					- the message to be sent
	 * @return
	 * 					- an indication of success
	 */
	private boolean routeTheMessage(JSONObject message) {
			String destination = (String) message.get("destination");
			String destEntity = destination.split(
					AgentWave.ADDRESS_SEPARATOR)[0];

			WebSocket destinationWebSocket;
			destinationWebSocket = entityToWebSocket.get(destEntity);
			if(destinationWebSocket != null) {
				destinationWebSocket.send(message.toString());
				li("Sent to agent: []. ", message.toString());
				return true;
			}

			destinationWebSocket = nodeToWebSocket.get(destEntity);
			if(destinationWebSocket != null) {
				destinationWebSocket.send(message.toString());
				li("Sent to node: []. ", message.toString());
				return true;
			}

			le("Failed to find the entity [] websocket.", destEntity);
			return false;
	}


	public WebSocketServerEntity(int serverAddress)
	{
		li("Started");
		webSocketServer = new WebSocketServer(new InetSocketAddress(serverAddress)) {
			
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
			{
				/*
				 * This method sends a message to the new client.
				 */
				li("new client connected []", webSocket);
			}
			
			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b)
			{
				li(("[] closed with exit code " + i), webSocket);
			}

			/**
			 * Receives message from a {@link WebSocketClient}.
			 * Messages can be:
			 * 					- entity registration message
			 * 					- message from one entity to another
			 *
			 * @param webSocket
			 * 					- the sender {@link WebSocket} client
			 * @param s
			 * 					- the JSON string containing a message and routing information
			 */
			@Override
			public void onMessage(WebSocket webSocket, String s)
			{
				Object obj = JSONValue.parse(s);
				if(obj == null) return;
				JSONObject message = (JSONObject) obj;

				// message in transit through the server
				if(message.get("destination") != null && routeTheMessage(message))
					return;

				if(message.get("nodeName") == null) {
					le("No nodeName in message");
					return;
				}
				String nodeName = (String)message.get("nodeName");

				//node registration message
                if(!nodeToWebSocket.containsKey(nodeName)) {
					nodeToWebSocket.put(nodeName, webSocket);
					nodeToEntities.put(nodeName, new ArrayList<>());
					li("Registered node []. ", nodeName);
				}

				// entity registration message
				String newEntity;
				if(message.get("entityName") != null)
				{
					newEntity = (String)message.get("entityName");
					if(!entityToWebSocket.containsKey(newEntity)) {
						entityToWebSocket.put(newEntity, webSocket);
						nodeToEntities.get(nodeName).add(newEntity);
					}
					li("Registered entity []. ", newEntity);
					printState();
					return;
				}
			}
			
			@Override
			public void onError(WebSocket webSocket, Exception e)
			{
				e.printStackTrace();
			}
			
			@Override
			public void onStart()
			{
				li("Server started successfully.");
			}

			private void printState() {
				li("###entities:  " + entityToWebSocket.keySet());
				li("###nodes: " + nodeToEntities.keySet());
				li("###entities: " + nodeToEntities.values());
			}
		};
		webSocketServer.setReuseAddr(true);
	}
	
	@Override
	public boolean start()
	{
		webSocketServer.start();
		running = true;
		return true;
	}

	/**
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	@Override
	public boolean stop()
	{
		try
		{
			webSocketServer.stop(SERVER_STOP_TIME);
			running = false;
			return true;
		} catch(InterruptedException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean isRunning()
	{
		return running;
	}
	
	@Override
	public String getName()
	{
		return null;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean addContext(EntityProxy context)
	{
		return false;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean removeContext(EntityProxy context)
	{
		return false;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean removeGeneralContext(EntityProxy context)
	{
		return false;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public EntityProxy asContext()
	{
		return null;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean addGeneralContext(EntityProxy context)
	{
		return false;
	}
}
