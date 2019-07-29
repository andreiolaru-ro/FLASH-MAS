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

import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.ShardContext;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;

/**
 * Simple support implementation that allows agents to send messages locally (inside the same JVM) based simply on agent
 * name.
 * 
 * @author Andrei Olaru
 */
public class LocalSupport extends DefaultPylonImplementation
{
	public static final String LOCAL_SUPPORT_NAME = "Local pylon";

	protected HashMap<String, MessageReceiver> messageReceivers = new HashMap<String, MessageReceiver>();

	public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {

		@Override
		public boolean send(String source, String destination, String content) {
			String agentName = getAgentNameFromAddress(getAgentAddress(destination));
			if (!messageReceivers.containsKey(agentName))
				return false;
			messageReceivers.get(agentName).receive(source, destination, content);
			return true;
		}

		@Override
		public boolean register(String agentName, MessageReceiver receiver) {
			messageReceivers.put(agentName, receiver);
			return true;
		}
	};

	/**
	 * Simple implementation of {@link MessagingShard}, that uses agents' names as their addresses.
	 * 
	 * @author Andrei Olaru
	 */
	public static class SimpleLocalMessaging extends MessagingShard
	{
		/**
		 * The serial UID.
		 */
		private static final long serialVersionUID = 1L;
		private MessagingPylonProxy pylon;
		private ShardContext agent;
		public MessageReceiver inbox;
		
		public SimpleLocalMessaging(ShardContext agent) {
			super();
			this.agent = agent;
			inbox = new MessageReceiver() {
				@Override
				public boolean receive(String source, String destination, String content) {
					receiveMessage(source, destination, content);
					return true;
				}
			};
		}

		public boolean register() {
			if (!(agent.getPylon() instanceof MessagingPylonProxy))
				throw new IllegalStateException("Pylon Context is not of expected type.");
			pylon = (MessagingPylonProxy) agent.getPylon();
			pylon.register(agent.getAgentName(), inbox);
			return true;
		}

		@Override
		public boolean sendMessage(String source, String destination, String content)
		{
			if (!(getAgent().getPylon() instanceof MessagingPylonProxy))
				throw new IllegalStateException("Platform Link is not of expected type");
			pylon.send(source, destination, content);
			return true;
		}

		@Override
		protected void receiveMessage(String source, String destination, String content)
		{
			super.receiveMessage(source, destination, content);
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
	 * The registry of agents that can receive messages, specifying the {@link MessagingShard} receiving the
	 * message.
	 *
	 */
	protected Map<String, SimpleLocalMessaging> registry = new HashMap<String, SimpleLocalMessaging>();
	
	/**
	 * If <code>true</code>, a separate thread will be used to buffer messages. Otherwise, only method calling will be
	 * used.
	 * <p>
	 * <b>WARNING:</b> not using a thread may lead to race conditions and deadlocks. Use only if you know what you are
	 * doing.
	 */
	protected boolean useThread = true;
	
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this queue is used to gather
	 * messages.
	 */
	protected LinkedBlockingQueue<Map.Entry<SimpleLocalMessaging, Vector<String>>> messageQueue = null;
	
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this is a reference to that
	 * thread.
	 */
	protected Thread messageThread = null;
	
	@Override
	public boolean start()
	{
		if(!super.start())
			return false;
		if(useThread)
		{
			messageQueue = new LinkedBlockingQueue<Map.Entry<SimpleLocalMessaging, Vector<String>>>();
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
	public String getRecommendedShardImplementation(AgentShardDesignation componentName) {
		if (componentName == AgentShardDesignation.standardFeature(StandardAgentShard.MESSAGING))
			return SimpleLocalMessaging.class.getName();
		return super.getRecommendedShardImplementation(componentName);
	}
	
	@Override
	public String getName()
	{
		return LOCAL_SUPPORT_NAME;
	}
}
