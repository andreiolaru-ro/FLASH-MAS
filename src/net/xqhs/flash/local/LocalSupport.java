/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.local;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import monitoringAndControl.websockets.WebSocketClientProxy;
import org.json.simple.JSONObject;

/**
 * Simple support implementation that allows agents to send messages locally (inside the same JVM) based simply on agent
 * name.
 * 
 * @author Andrei Olaru
 */
public class LocalSupport extends DefaultPylonImplementation
{
	/**
	 * The type of this support infrastructure (its 'kind')
	 */
	public static final String					LOCAL_SUPPORT_NAME	= "Local pylon";
	
	/**
	 * The receivers for each endpoint.
	 */
	protected HashMap<String, MessageReceiver>	messageReceivers	= new HashMap<>();

	/**
	 * The id of that node.
	 * */

	protected String nodeId                                         = null;

	/*
	* Whether this pylon is part of central node for monitoring and control.
	* */

	protected boolean isCentralNode;

	/**
	 * The proxy to the webSocket server; this is actually a webSocket client.
	 */
	protected WebSocketClientProxy webSocketClient;


	/**
	 * The server address itself.
	 */
	protected String                  serverAddress;


	public MessagingPylonProxy					messagingProxy		= new MessagingPylonProxy() {
																		
				@Override
				public boolean send(String source,
						String destination, String content)
				{
					String agentName = destination.split(
							AgentWave.ADDRESS_SEPARATOR)[0];
					if(!messageReceivers.containsKey(agentName))
					{
						JSONObject msgToServer = new JSONObject();
						msgToServer.put("nodeName", nodeId);
						msgToServer.put("simpleDest", agentName);
						msgToServer.put("source", source);
						msgToServer.put("destination", destination);
						msgToServer.put("content", content);

						webSocketClient.send(msgToServer.toString());
						return true;
					}
					messageReceivers.get(agentName).receive(
							source, destination, content);
					return true;
				}
																		
				@Override
				public boolean register(String agentName,
						MessageReceiver receiver)
				{
					messageReceivers.put(agentName, receiver);
					webSocketClient.addReceiverAgent(agentName, receiver);

					if(agentName.equals(nodeId)) return true;

					JSONObject agentRegMsg = new JSONObject();
					agentRegMsg.put("nodeName", nodeId);
					agentRegMsg.put("agentName", agentName);

					webSocketClient.send(agentRegMsg.toString());
					return true;
				}

				@Override
				public String getRecommendedShardImplementation(
						AgentShardDesignation shardType)
				{
					return LocalSupport.this
							.getRecommendedShardImplementation(
									shardType);
				}

				@Override
				public String getEntityName()
				{
					return getName();
				}
			};


	public void registerNodeId(String nodeId) {
		this.nodeId = nodeId;
		JSONObject msg = new JSONObject();
		msg.put("nodeName", nodeId);
		msg.put("isCentral", isCentralNode);

		webSocketClient.send(msg.toString());
	}

	public void setIsCentralNode(boolean isCentralNode) {
		this.isCentralNode = isCentralNode;
	}

	public LocalSupport() {
		super();
		try {
			webSocketClient = new WebSocketClientProxy(new URI("ws://localhost:8885"));
			webSocketClient.connect();
			Thread.sleep(1000);
		} catch (URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simple implementation of {@link AbstractMessagingShard}, that uses agents' names as their addresses.
	 *
	 * @author Andrei Olaru
	 */
	public static class SimpleLocalMessaging extends AbstractNameBasedMessagingShard
	{
		/**
		 * The serial UID.
		 */
		private static final long	serialVersionUID	= 1L;
		/**
		 * Reference to the local pylon proxy.
		 */
		private MessagingPylonProxy	pylon;
		/**
		 * The {@link MessageReceiver} instance of this shard.
		 */
		public MessageReceiver		inbox;
		
		/**
		 * Default constructor.
		 */
		public SimpleLocalMessaging()
		{
			super();
			inbox = new MessageReceiver() {
				@Override
				public void receive(String source, String destination, String content)
				{
					receiveMessage(source, destination, content);
				}
			};
		}
		
		/**
		 * Relay for the supertype method.
		 */
		@Override
		protected void receiveMessage(String source, String destination, String content)
		{
			super.receiveMessage(source, destination, content);
		}
		
		@Override
		public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
		{
			if(!(context instanceof MessagingPylonProxy))
				throw new IllegalStateException("Pylon Context is not of expected type.");
			pylon = (MessagingPylonProxy) context;
			pylon.register(getAgent().getEntityName(), inbox);
			return true;
		}
		
		@Override
		public boolean sendMessage(String source, String destination, String content)
		{
			return pylon.send(source, destination, content);
		}
	}
	
	/**
	 * The thread that manages the message queue.
	 * 
	 * @author Andrei Olaru
	 */
	class MessageThread implements Runnable
	{
		@Override
		public void run()
		{
			// System.out.println("oops");
			while(useThread)
			{
				if(messageQueue.isEmpty())
					try
					{
						synchronized(messageQueue)
						{
							messageQueue.wait();
						}
					} catch(InterruptedException e)
					{
						// do nothing
					}
				else
				{
					Entry<SimpleLocalMessaging, Vector<String>> event = messageQueue.poll();
					event.getKey().receiveMessage(event.getValue().get(0), event.getValue().get(1),
							event.getValue().get(2));
				}
			}
		}
	}
	
	/**
	 * The registry of agents that can receive messages, specifying the {@link AbstractMessagingShard} receiving the
	 * message.
	 *
	 */
	protected Map<String, SimpleLocalMessaging>										registry		= new HashMap<>();
	
	/**
	 * If <code>true</code>, a separate thread will be used to buffer messages. Otherwise, only method calling will be
	 * used.
	 * <p>
	 * <b>WARNING:</b> not using a thread may lead to race conditions and deadlocks. Use only if you know what you are
	 * doing.
	 */
	protected boolean																useThread		= true;
	
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this queue is used to gather
	 * messages.
	 */
	protected LinkedBlockingQueue<Map.Entry<SimpleLocalMessaging, Vector<String>>>	messageQueue	= null;
	
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this is a reference to that
	 * thread.
	 */
	protected Thread																messageThread	= null;
	
	@Override
	public boolean start()
	{
		if(!super.start())
			return false;
		if(useThread)
		{
			messageQueue = new LinkedBlockingQueue<>();
			messageThread = new Thread(new MessageThread());
			messageThread.start();
		}
		return true;
	}
	
	@Override
	public boolean stop()
	{
		super.stop();
		if(useThread)
		{
			useThread = false; // signal to the thread
			synchronized(messageQueue)
			{
				messageQueue.clear();
				messageQueue.notifyAll();
			}
			try
			{
				messageThread.join();
			} catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			messageQueue = null;
			messageThread = null;
		}
		return true;
	}
	
	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardName)
	{
		if(shardName.equals(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING)))
			return SimpleLocalMessaging.class.getName();
		return super.getRecommendedShardImplementation(shardName);
	}
	
	@Override
	public String getName()
	{
		return LOCAL_SUPPORT_NAME;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Pylon> asContext()
	{
		return messagingProxy;
	}
}
