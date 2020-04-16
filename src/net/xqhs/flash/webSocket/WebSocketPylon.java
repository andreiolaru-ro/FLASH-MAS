package net.xqhs.flash.webSocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.flash.core.DeploymentConfiguration;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class WebSocketPylon extends DefaultPylonImplementation {
	
	class MessageThread implements Runnable {
		@Override
		public void run() {
			// System.out.println("oops");
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
		/*
		 * The agent is registered within the webSocket client.
		 */
		@Override
		public boolean register(String agentName, MessageReceiver receiver) {
			webSocketClient.addReceiverAgent(agentName, receiver);
			JSONObject messageToServer = new JSONObject();
			messageToServer.put("nodeName", nodeName);
			messageToServer.put("agentName", agentName);
			webSocketClient.send(messageToServer.toString());
			return true;
		}
		
		@Override
		public boolean send(String source, String destination, String content) {
			JSONObject messageToServer = new JSONObject();
			messageToServer.put("nodeName", nodeName);
			messageToServer.put("source", source);
			messageToServer.put("destination", destination);
			messageToServer.put("content", content);
			
			webSocketClient.send(messageToServer.toString());
			return true;
		}

		@Override
		public void registerNode(String id, boolean isCentral) {
			nodeName = id;
			isCentralNode = isCentral;
			JSONObject messageToServer = new JSONObject();
			messageToServer.put("nodeName", nodeName);
			messageToServer.put("isCentral", isCentralNode);
			webSocketClient.send(messageToServer.toString());
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
	
	protected boolean				hasServer		= false;
	protected int					serverPort		= -1;
	protected WebSocketServerEntity	serverEntity	= null;

	protected String                nodeName;

	protected boolean               isCentralNode;
	
	/**
	 * The server address itself.
	 */
	protected String webSocketServerAddressName;
	
	/**
	 * The proxy to the webSocket server; this is actually a webSocket client.
	 */
	protected WebSocketClientProxy webSocketClient;
	
	protected Map<String, WebSocketMessagingShard> registry = new HashMap<>();
	
	protected boolean useThread = true;
	
	protected Queue<Map.Entry<WebSocketMessagingShard, Vector<String>>> messageQueue = null;
	
	protected Thread messageThread = null;
	
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
			// Thread.sleep(1000);
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
		return true;
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
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
