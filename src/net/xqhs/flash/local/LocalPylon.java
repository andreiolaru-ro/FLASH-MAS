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

/**
 * Simple support implementation that allows agents to send messages locally (inside the same JVM) based simply on agent
 * name.
 * 
 * @author Andrei Olaru
 */
public class LocalPylon extends DefaultPylonImplementation {
	/**
	 * The type of this support infrastructure (its 'kind')
	 */
	public static final String LOCAL_SUPPORT_NAME = "Local pylon";
	/**
	 * The receivers for each endpoint.
	 */
	protected HashMap<String, MessageReceiver>	messageReceivers	= new HashMap<>();

	protected String nodeName;

	protected boolean isPylonOnCentralNode;
	/**
	 * The proxy to this entity.
	 */
	public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {

		@Override
		public boolean send(String source, String destination, String content) {
			String agentName = destination.split(AgentWave.ADDRESS_SEPARATOR)[0];
			if(!messageReceivers.containsKey(agentName))
				return false;
			messageReceivers.get(agentName).receive(source, destination, content);
			return true;
		}

		@Override
		public boolean register(String agentName, MessageReceiver receiver) {
			messageReceivers.put(agentName, receiver);
			return true;
		}

		@Override
		public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
			return LocalPylon.this.getRecommendedShardImplementation(shardType);
		}

		@Override
		public String getEntityName() {
			return getName();
		}

		@Override
		public void registerNode(String id, boolean isCentralNode) {
			nodeName = id;
			isPylonOnCentralNode = isCentralNode;
		}

	};
	
	/**
	 * Simple implementation of {@link AbstractMessagingShard}, that uses agents' names as their addresses.
	 *
	 * @author Andrei Olaru
	 */
	public static class SimpleLocalMessaging extends AbstractNameBasedMessagingShard {
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
		public SimpleLocalMessaging() {
			super();
			inbox = new MessageReceiver() {
				@Override
				public void receive(String source, String destination, String content) {
					receiveMessage(source, destination, content);
				}
			};
		}
		
		/**
		 * Relay for the supertype method.
		 */
		@Override
		protected void receiveMessage(String source, String destination, String content) {
			super.receiveMessage(source, destination, content);
		}
		
		@Override
		public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
			if(!(context instanceof MessagingPylonProxy))
				throw new IllegalStateException("Pylon Context is not of expected type.");
			pylon = (MessagingPylonProxy) context;
			pylon.register(getAgent().getEntityName(), inbox);
			return true;
		}
		
		@Override
		public boolean sendMessage(String source, String destination, String content) {
			if(pylon == null) { // FIXME: use logging
				System.out.println("No pylon added as context.");
				return false;
			}
			pylon.send(source, destination, content);
			return true;
		}

		@Override
		public void registerNode(String nodeName, boolean isCentral) {
			pylon.registerNode(nodeName, isCentral);
		}
	}
	
	/**
	 * The thread that manages the message queue.
	 * 
	 * @author Andrei Olaru
	 */
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
					Entry<SimpleLocalMessaging, Vector<String>> event = messageQueue.poll();
					event.getKey().receiveMessage(event.getValue().get(0), event.getValue().get(1),
							event.getValue().get(2));
				}
			}
		}
	}

	/**
	 * If <code>true</code>, a separate thread will be used to buffer messages. Otherwise, only method calling will be
	 * used.
	 * <p>
	 * <b>WARNING:</b> not using a thread may lead to race conditions and deadlocks. Use only if you know what you are
	 * doing.
	 */
	protected boolean																useThread			= true;
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this is a reference to that
	 * thread.
	 */
	protected Thread																messageThread		= null;
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this queue is used to gather
	 * messages.
	 */
	protected LinkedBlockingQueue<Map.Entry<SimpleLocalMessaging, Vector<String>>>	messageQueue		= null;

	@Override
	public boolean start() {
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
		return true;
	}
	
	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardName) {
		if(shardName.equals(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING)))
			return SimpleLocalMessaging.class.getName();
		return super.getRecommendedShardImplementation(shardName);
	}
	
	@Override
	public String getName() {
		return LOCAL_SUPPORT_NAME;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Pylon> asContext() {
		return messagingProxy;
	}
}
