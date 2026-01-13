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

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.Dispatcher;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.LoggerClassic;

/**
 * WebSocket support implementation that allows agents to send messages whether they are inside the same JVM or not. The
 * pylon is connected to a {@link WebSocketServerEntity}. Therefore any agent in the context of this support is able to
 * send messages to agents located on any other pylon connected to the same Websocket server.
 *
 * @author Florina Nastasoiu
 * @author Andrei Olaru
 */
public class WebSocketPylon extends DefaultPylonImplementation {
	/**
	 * The serial UID.
	 */
	private static final long	serialVersionUID				= 1L;
	/**
	 * If this key is present, the entity will be unregistered.
	 */
	public static final String	UNREGISTER_KEY					= "unregister";
	/**
	 * The attribute name of server address of this instance.
	 */
	public static final String	WEBSOCKET_SERVER_ADDRESS_NAME	= "connectTo";
	/**
	 * The attribute name for the server port.
	 */
	public static final String	WEBSOCKET_SERVER_PORT_NAME		= "serverPort";
	/**
	 * The prefix for Websocket server address.
	 */
	public static final String	WS_PROTOCOL_PREFIX				= "ws://";
	
	/**
	 * The proxy to this pylon, to be referenced by any entities in the scope of this pylon.
	 */
	public MessagingPylonProxy				messagingProxy		= null;
	/**
	 * <code>true</code> if there is a Websocket server configured on the local node.
	 */
	protected boolean						hasServer;
	/**
	 * For the case in which a server must be created on this node, the port the server is bound to.
	 */
	protected int							serverPort			= -1;
	/**
	 * For the case in which a server must be created on this node, the entity that represents the server.
	 */
	protected WebSocketServerEntity			serverEntity;
	/**
	 * The address of the Websocket server that the client should connect to.
	 */
	protected String						webSocketServerAddress;
	/**
	 * The {@link WebSocketClient} instance to use.
	 */
	protected WebSocketClient				webSocketClient;
	/**
	 * For the entities in the scope of this pylon, the correspondence between their names and their
	 * {@link WaveReceiver} instances.
	 */
	protected HashMap<String, WaveReceiver>	messageReceivers	= new HashMap<>();
	/**
	 * The dispatcher for outgoing messages.
	 */
	protected Dispatcher<AgentWave>			dispatcher;
	/**
	 * The index of the in-bound messages queue with the {@link #dispatcher}.
	 */
	protected Integer						INBOUND				= null;
	/**
	 * The index of the out-bound messages queue with the {@link #dispatcher}.
	 */
	protected Integer						OUTBOUND			= null;
	
	/**
	 * The constructor, with the mission of building the {@link MessagingPylonProxy}.
	 */
	public WebSocketPylon() {
		dispatcher = new Dispatcher<>(getLogger());
		OUTBOUND = Integer.valueOf(dispatcher.addProcessor(this::sendMessage, true));
		INBOUND = Integer.valueOf(dispatcher.addProcessor(this::receiveMessage));
		
		messagingProxy = new WaveMessagingPylonProxy() {
			@Override
			public boolean register(String entityName, WaveReceiver receiver) {
				return registerEntity(entityName, receiver);
			}
			
			@Override
			public boolean unregister(String entityName, WaveReceiver registeredReceiver) {
				return unregisterEntity(entityName, registeredReceiver);
			}
			
			@Override
			public boolean send(AgentWave wave) {
				dispatcher.add(OUTBOUND.intValue(), wave);
				return true;
			}
			
			@Override
			public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
				return WebSocketPylon.this.getRecommendedShardImplementation(shardType);
			}
			
			@Override
			public String getEntityName() {
				return getName();
			}
		};
	}
	
	/**
	 * Starts the {@link WebSocketServerEntity} if the pylon was delegated from the deployment. Now the entity is ready
	 * to send and receive messages.
	 *
	 * @return an indication of success.
	 */
	@Override
	public boolean start() {
		if(hasServer) {
			serverEntity = new WebSocketServerEntity(serverPort);
			if(!serverEntity.start())
				return ler(false, "Server entity failed to start");
		}
		LoggerClassic log = getLogger();
		PlatformUtils.tryFor(getLogger(), "connection to WebSocket server at " + webSocketServerAddress, () -> {
			webSocketClient = new WebSocketClient(new URI(webSocketServerAddress)) {
				@Override
				public void onOpen(ServerHandshake serverHandshake) {
					log.lf("connected to []", getURI());
					dispatcher.resume(OUTBOUND.intValue());
				}
				
				@Override
				public void onMessage(String string) {
					receiveMessage(string);
				}
				
				@Override
				public void onClose(int i, String s, boolean b) {
					log.lw("Closed with exit code " + i);
					dispatcher.suspend(OUTBOUND.intValue());
				}
				
				@Override
				public void onError(Exception e) {
					log.le(Arrays.toString(e.getStackTrace()));
				}
			};
			if(webSocketClient.connectBlocking())
				return Boolean.TRUE;
			return Boolean.FALSE;
		});
		
		if(!super.start())
			return false;
		// li("Started" + (useThread ? " [with thread]." : "[without thread]"));
		return true;
	}
	
	@Override
	public boolean stop() {
		dispatcher.stop();
		super.stop();
		
		if(hasServer)
			serverEntity.stop();
		try {
			webSocketClient.closeBlocking();
		} catch(InterruptedException e) {
			le("Failed to close client:", PlatformUtils.printException(e));
		}
		li("Stopped");
		return true;
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.isSimple(WEBSOCKET_SERVER_PORT_NAME)) {
			hasServer = true;
			serverPort = Integer.parseInt(configuration.getAValue(WEBSOCKET_SERVER_PORT_NAME));
			webSocketServerAddress = WS_PROTOCOL_PREFIX + PlatformUtils.getLocalHostURI() + ":" + serverPort;
		}
		else if(configuration.isSimple(WEBSOCKET_SERVER_ADDRESS_NAME))
			webSocketServerAddress = configuration.getAValue(WEBSOCKET_SERVER_ADDRESS_NAME);
		if(configuration.isSimple(DeploymentConfiguration.NAME_ATTRIBUTE_NAME))
			name = configuration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		return true;
	}
	
	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardName) {
		if(shardName.equals(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)))
			return WebSocketMessagingShard.class.getName();
		return super.getRecommendedShardImplementation(shardName);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Pylon> asContext() {
		return messagingProxy;
	}
	
	/**
	 * The entity is both:
	 * <ul>
	 * <li>registered within the local instance which is useful for routing a message back to the the
	 * {@link WaveReceiver} instance when it arrives from the server
	 * <li>registered to the {@link WebSocketServerEntity} using an entity registration format message which is sent by
	 * the local client
	 * </ul>
	 *
	 * @param entityName
	 *            - the name of the entity
	 * @param receiver
	 *            - the {@link WaveReceiver} instance to receive messages
	 * @return an indication of success
	 */
	protected boolean registerEntity(String entityName, WaveReceiver receiver) {
		if(messageReceivers.containsKey(entityName))
			return ler(false, "Entity [] already registered with this pylon.", entityName);
		messageReceivers.put(entityName, receiver);
		
		AgentWave waveToServer = new AgentWave(entityName).addSourceElements(nodeName);
		try {
			dispatcher.add(OUTBOUND.intValue(), waveToServer);
			lf("Registered entity []/[] with this pylon: ", entityName, receiver, waveToServer);
			return true;
		} catch(Exception e) {
			le("Failed to send message:", PlatformUtils.printException(e));
			return false;
		}
	}
	
	/**
	 * The entity is unregistered from the local instance and the server.
	 * 
	 * @param entityName
	 *            - the name of the entity
	 * @param registeredReceiver
	 *            - the {@link WaveReceiver} instance to receive messages
	 * @return an indication of success
	 */
	protected boolean unregisterEntity(String entityName, WaveReceiver registeredReceiver) {
		if(!messageReceivers.remove(entityName, registeredReceiver))
			return false;
		
		AgentWave waveToServer = (AgentWave) new AgentWave(entityName).addSourceElements(nodeName).add(UNREGISTER_KEY,
				UNREGISTER_KEY);
		try {
			dispatcher.add(OUTBOUND.intValue(), waveToServer);
			lf("Unregistered entity [] from this pylon: ", entityName, waveToServer);
			return true;
		} catch(Exception e) {
			le("Failed to send message:", PlatformUtils.printException(e));
			return false;
		}
	}
	
	/**
	 * Sends the given wave.
	 * 
	 * @param wave
	 *            - the {@link AgentWave} to send.
	 * @return an indication of success.
	 */
	protected boolean sendMessage(AgentWave wave) {
		if(messageReceivers.containsKey(wave.getFirstDestinationElement())) {
			messageReceivers.get(wave.getFirstDestinationElement()).receive(wave);
			return true;
		}
		try {
			webSocketClient.send(wave.toSerializedString());
		} catch(Exception e) {
			le("Failed to send message []:", wave, PlatformUtils.printException(e));
		}
		return true;
	}
	
	/**
	 * Receives a message from the server. The message was previously routed to this websocket client address and no
	 * must be de-serialized and routed internally.
	 *
	 * @param waveString
	 *            - the string containing the serialized message.
	 */
	protected void receiveMessage(String waveString) {
		AgentWave wave;
		try {
			wave = (AgentWave) MultiValueMap.fromSerializedString(waveString);
			dispatcher.add(INBOUND.intValue(), wave);
		} catch(Exception e) {
			le("Exception [] when parsing []", e, waveString);
		}
	}
	
	/**
	 * Process a wave destined to an entity in this pylon. It is further routed to a specific entity using the
	 * appropriate {@link WaveReceiver} instance. The entity is searched within the context of this pylon.
	 * 
	 * @param wave
	 *            - the wave to process.
	 */
	protected void receiveMessage(AgentWave wave) {
		String destination = wave.getFirstDestinationElement();
		if(!messageReceivers.containsKey(destination) || messageReceivers.get(destination) == null)
			le("Entity [] does not exist in the scope of this pylon.", destination);
		else
			messageReceivers.get(destination).receive(wave);
	}
}
