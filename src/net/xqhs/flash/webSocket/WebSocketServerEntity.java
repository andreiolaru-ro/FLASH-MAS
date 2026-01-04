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
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import net.xqhs.util.logging.Logger;

import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.LoggerClassic;

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
public class WebSocketServerEntity extends EntityCore<Pylon> {
	/**
	 * The serial UID.
	 */
	private static final long			serialVersionUID	= 1L;
	/**
	 * Timeout for stopping the server (sent directly to {@link WebSocketServer#stop(int)}.
	 */
	private static final int			SERVER_STOP_TIME	= 10;
	/**
	 * The {@link WebSocketServer} instance.
	 */
	private WebSocketServer				webSocketServer;
	/**
	 * Map all entities to their {@link WebSocket}.
	 */
	private HashMap<String, WebSocket>	entityToWebSocket	= new HashMap<>();
	
	{
		setUnitName("websocket-server");
		setLoggerType(PlatformUtils.platformLogType());
		setLogLevel(Logger.Level.ALL);
	}
	
	/**
	 * Creates a Websocket server instance. It must be started with {@link #start()}.
	 * 
	 * @param serverPort
	 *            - the port on which to start the server.
	 */
	public WebSocketServerEntity(int serverPort) {
		lf("Starting websocket server on port: ", Integer.valueOf(serverPort));
		LoggerClassic log = getLogger();
		webSocketServer = new WebSocketServer(new InetSocketAddress(serverPort)) {
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
				log.li("new client connected []", webSocket);
			}
			
			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b) {
				log.li("[] closed with exit code ", webSocket, Integer.valueOf(i));
			}
			
			/**
			 * Receives message from a {@link WebSocketClient} and sends it directly to
			 * {@link WebSocketServerEntity#processMessage}.
			 * 
			 * @param webSocket
			 *            - the sender {@link WebSocket} client
			 * @param string
			 *            - the string containing the serialized wave.
			 */
			@Override
			public void onMessage(WebSocket webSocket, String string) {
				processMessage(webSocket, string);
			}
			
			@Override
			public void onError(WebSocket webSocket, Exception e) {
				e.printStackTrace();
			}
			
			@Override
			public void onStart() {
				log.li("Server started successfully.");
			}
		};
		webSocketServer.setReuseAddr(true);
	}
	
	/**
	 * Processes a message. Messages can be:
	 * <ul>
	 * <li>message from one entity to another - contains a <code>destination</code> field <i>and</i> can be routed.
	 * <li>entity registration message. It must contain a <code>nodeName</code> field. If it registers an entity, it
	 * must also contain an <code>entityName</code> field.
	 * </ul>
	 * The state will be printed after each registration.
	 *
	 * @param webSocket
	 *            - the sender {@link WebSocket} client
	 * @param waveString
	 *            - the string containing the serialized {@link AgentWave}
	 */
	protected void processMessage(WebSocket webSocket, String waveString) {
		AgentWave wave;
		try {
			wave = (AgentWave) MultiValueMap.fromSerializedString(waveString);
		} catch(Exception e) {
			le("Exception [] when parsing []", e, waveString);
			return;
		}
		
		if(wave.containsKey(AgentWave.DESTINATION_ELEMENT)) {
			// message in transit through the server, must be routed
			String destination = wave.getFirstDestinationElement();
			if(destination != null) {
				WebSocket destinationWebSocket;
				destinationWebSocket = entityToWebSocket.get(destination);
				if(destinationWebSocket != null) {
					destinationWebSocket.send(waveString);
					lf("Sent to entity: []. ", wave);
					return;
				}
				le("Failed to find websocket for the entity [].", destination);
				return;
			}
		}
		
		String nodeName = wave.getFirstSource();
		// consider as entity registration message
		if(nodeName == null)
			lw("nodeName is missing in []", wave);
		String entityName = wave.getContent();
		if(entityName != null) {
			// TODO: corner case when an entity is registered from the same WSPylon but a different node??
			// TODO: unregister old location?
			if(wave.containsKey(WebSocketPylon.UNREGISTER_KEY)) {
				if(!entityToWebSocket.containsKey(entityName))
					lw("Entity [] not registered on this server.", entityName);
				else if(entityToWebSocket.get(entityName) != webSocket)
					lw("Entity [] not registered on this server under that pylon: ", entityName, webSocket);
				else {
					entityToWebSocket.remove(entityName, webSocket);
					lf("Unregistered entity [] on []. ", entityName, nodeName);
				}
			}
			else {
				if(!entityToWebSocket.containsKey(entityName) || entityToWebSocket.get(entityName) != webSocket)
					entityToWebSocket.put(entityName, webSocket);
				lf("Registered entity [] on []. ", entityName, nodeName);
			}
		}
		else
			le("Message could not be used []", wave);
		printState();
	}
	
	/**
	 * Logs (fine level) the state of the server, as the lists of entities known, nodes known, and correspondence
	 * between nodes and entities.
	 */
	protected void printState() {
		lf("entities: [] ", entityToWebSocket.keySet());
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		webSocketServer.start();
		return true;
	}
	
	@Override
	public boolean stop() {
		try {
			webSocketServer.stop(SERVER_STOP_TIME);
			li("server successfully stopped.");
			return super.stop();
		} catch(InterruptedException e) {
			return ler(false, "stopping failed: ", PlatformUtils.printException(e));
		}
	}
}
