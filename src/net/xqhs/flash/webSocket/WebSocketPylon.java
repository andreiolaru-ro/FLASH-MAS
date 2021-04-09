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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.Node.NodeProxy;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * WebSocket support implementation that allows agents to send messages whether they are inside the same JVM or not.
 * The support has a {@link WebSocketClientProxy} which is connected to the {@link WebSocketServerEntity}. Therefore
 * any agent in the context of this support is able to send messages to agents located on any other support.
 *
 *  @author Florina Nastasoiu
 */
public class WebSocketPylon extends DefaultPylonImplementation {
	
	class MessageThread implements Runnable {
		@Override
		public void run() {
			while(useThread) {
				if(messageQueue.isEmpty())
					try {
						synchronized(messageQueue) {
							messageQueue.wait();
						}
					} catch(InterruptedException e) {
						// do nothing
					}
				else {
					Map.Entry<WebSocketMessagingShard, Vector<String>> event = messageQueue.poll();
					event.getKey().receiveMessage(event.getValue().get(0), event.getValue().get(1),
							event.getValue().get(2));
				}
			}
		}
	}
	
	public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {
		/**
		 * The entity is both:
		 * 					- registered within the {@link WebSocketClientProxy} local instance which is useful for
		 * 					  routing a message back to the the {@link MessageReceiver} instance when it arrives from
		 * 					  the server
		 * 					- registered to the {@link WebSocketServerEntity} using an entity registration format message
		 * 				      which is sent by the local {@link WebSocketClientProxy} client
		 *
		 * @param entityName
		 * 					- the name of the entity
		 * @param receiver
		 * 					- the {@link MessageReceiver} instance to receive messages
		 * @return
		 * 					- an indication of success
		 */
		@Override
		@SuppressWarnings("unchecked")
		public boolean register(String entityName, MessageReceiver receiver) {
			webSocketClient.addReceiverAgent(entityName, receiver);
			JSONObject messageToServer = new JSONObject();
			messageToServer.put("nodeName", nodeName);
			messageToServer.put("entityName", entityName);
			webSocketClient.send(messageToServer.toString());
			return true;
		}

		/**
		 * Send a message to the server.
		 *
		 * @param source
		 * 					- the source endpoint
		 * @param destination
		 * 					- the destination endpoint
		 * @param content
		 * 					- the content of the message
		 * @return
		 * 					- an indication of success
		 */
		@Override
		@SuppressWarnings("unchecked")
		public boolean send(String source, String destination, String content) {
			if(webSocketClient.messageReceivers.containsKey(destination)) {
				webSocketClient.messageReceivers.get(destination).receive(source, destination, content);
				return true;
			}
			JSONObject messageToServer = new JSONObject();
			messageToServer.put("nodeName", nodeName);
			messageToServer.put("source", source);
			messageToServer.put("destination", destination);
			messageToServer.put("content", content);
			webSocketClient.send(messageToServer.toString());
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

	/**
	 * The attribute name of server address of this instance.
	 */
	public static final String		WEBSOCKET_SERVER_ADDRESS_NAME	= "connectTo";
	/**
	 * The attribute name for the server port.
	 */
	public static final String		WEBSOCKET_SERVER_PORT_NAME		= "serverPort";
	
	protected boolean				hasServer;
	protected int					serverPort		= -1;
	protected WebSocketServerEntity	serverEntity;

	protected String                nodeName;

	/**
	 * The server address itself.
	 */
	protected String                webSocketServerAddressName;
	
	/**
	 * The proxy to the {@link WebSocketServerEntity} which has a webSocket client.
	 */
	protected WebSocketClientProxy  webSocketClient;

	protected boolean useThread = true;
	
	protected Queue<Map.Entry<WebSocketMessagingShard, Vector<String>>> messageQueue;
	
	protected Thread messageThread;

	/**
	 * Starts the {@link WebSocketServerEntity} if the pylon was delegated from the deployment and instantiates its
	 * local {@link WebSocketClientProxy which is further connected to the server.
	 * Now the entity is ready to send and receive messages.
	 *
	 * @return an indication of success.
	 */
	@Override
	public boolean start() {
		if(hasServer) {
			serverEntity = new WebSocketServerEntity(serverPort);
			serverEntity.start();
		}
		
		try {
			int tries = 10;
			long space = 1000;
			while(tries > 0) {
				try {
					webSocketClient = new WebSocketClientProxy(new URI(webSocketServerAddressName));
				} catch(URISyntaxException e) {
					e.printStackTrace();
					return false;
				}
				if(webSocketClient.connectBlocking())
					break;
				Thread.sleep(space);
				tries--;
				System.out.println("Tries:" + tries);
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		
		if(!super.start())
			return false;
		if(useThread) {
			messageQueue = new LinkedBlockingQueue<>();
			messageThread = new Thread(new MessageThread());
			messageThread.start();
		}
		li("Started" + (useThread ? " with thread." : ""));
		return true;
	}
	
	@Override
	public boolean stop() {
		super.stop();
		if(useThread) {
			useThread = false; // signal to the thread
			synchronized(messageQueue) {
				messageQueue.clear();
				messageQueue.notifyAll();
			}
			try {
				messageThread.join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			messageQueue = null;
			messageThread = null;
		}
		if(hasServer)
			serverEntity.stop();
		try {
			webSocketClient.close();
		} catch (InterruptedException x) {
			x.printStackTrace();
		}
		return true;
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.isSimple(WEBSOCKET_SERVER_ADDRESS_NAME))
			webSocketServerAddressName = configuration.getAValue(WEBSOCKET_SERVER_ADDRESS_NAME);
		if(configuration.isSimple(DeploymentConfiguration.NAME_ATTRIBUTE_NAME))
			name = configuration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		if(configuration.isSimple(WEBSOCKET_SERVER_PORT_NAME)) {
			hasServer = true;
			serverPort = Integer.parseInt(configuration.getAValue(WEBSOCKET_SERVER_PORT_NAME));
		}
		return true;
	}

	@Override
	public boolean addContext(EntityProxy<Node> context) {
		if(!super.addContext(context))
			return false;
		nodeName = context.getEntityName();
		lf("Added node context ", nodeName);
		return true;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<?> context) {
		if(context instanceof NodeProxy)
			return addContext((NodeProxy) context);
		return false;
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
}
