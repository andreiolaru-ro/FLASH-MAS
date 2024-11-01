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
package net.xqhs.flash.local;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.flash.core.RunnableEntity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.NameBasedMessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * Simple support implementation that allows agents to send messages locally (inside the same JVM) based simply on agent
 * name. This now uses {@link AgentWave}s (the previous implementation is kept in {@link LocalClassicPylon}).
 * <p>
 * Messages with destination which have not yet been registered (undeliverable messages) will be discarded.
 * <p>
 * There are two ways in which this implementation can work.
 * <p>
 * In the <i>direct</i> method (when a thread is <b>not</b> used), the send method leads directly to a call of the
 * {@link WaveReceiver#receive} method of the shard in the destination agent.
 * <p>
 * In the <i>queued</i> method, a thread is used (as configured by means of the {@link #USE_THREAD_PARAM_NAME} parameter
 * in the configuration), which processes a queue to which messages are added, and from which messages are taken to be
 * delivered.
 * <p>
 * For the safety and reliability of implementations which just want to use the default pylon, the default method is the
 * queued method, so deadlocks are avoided.
 * <p>
 * <b>Warning:</b> when using a thread, the {#send(String, String, String)} method always returns <code>true</code>, but
 * it is no guaranteed that the message has reached its destination.
 * <p>
 * TODO: implement keeping undeliverable messages (see {@link #KEEP_UNDELIVERABLE_PARAM_NAME} and
 * {@link #RETRY_EVERY_PARAM_NAME}).
 *
 * @author Andrei Olaru
 */
public class LocalPylon extends DefaultPylonImplementation implements RunnableEntity<Node> {
	/**
	 * An alias for {@link NameBasedMessagingShard}, kept for modeling purposes.
	 */
	public static class SimpleLocalMessaging extends NameBasedMessagingShard {
		/**
		 * Serial version UID.
		 */
		private static final long serialVersionUID = 1L;
		// nothing to add, but kept for compatibility with older tests and such.
	}
	
	/**
	 * The thread that manages the message queue.
	 * 
	 * @author Andrei Olaru
	 */
	class MessageThread implements Runnable {
		@Override
		public void run() {
			processQueue();
		}
	}
	
	/**
	 * The proxy to this entity.
	 */
	public WaveMessagingPylonProxy messagingProxy = new WaveMessagingPylonProxy() {
		@Override
		public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
			return LocalPylon.this.getRecommendedShardImplementation(shardType);
		}
		
		@Override
		public String getEntityName() {
			return getName();
		}
		
		@Override
		public boolean register(String entityName, WaveReceiver receiver) {
			messageReceivers.put(entityName, receiver);
			return true;
		}
		
		@Override
		public boolean unregister(String entityName, WaveReceiver registeredReceiver) {
			return messageReceivers.remove(entityName, registeredReceiver);
		}
		
		@Override
		public boolean send(AgentWave wave) {
			return LocalPylon.this.send(wave);
		}
	};
	
	/**
	 * The type of this support infrastructure (its 'kind')
	 */
	public static final String	LOCAL_SUPPORT_NAME				= "Local";
	/**
	 * Indicates whether a message queue and a message-processing thread should be used.
	 */
	public static final String	USE_THREAD_PARAM_NAME			= "use-thread";
	/**
	 * Indicates whether the messages where the destination is not known should be kept and delivery should be retried.
	 * The value of the parameter indicates the number of retries (see {@link LocalPylon} for more details).
	 */
	public static final String	KEEP_UNDELIVERABLE_PARAM_NAME	= "keep-undeliverable-for";
	/**
	 * Indicates how often delivery should be retried for messages.
	 */
	public static final String	RETRY_EVERY_PARAM_NAME			= "retry-every";
	
	/**
	 * If <code>true</code>, a separate thread will be used to buffer messages. Otherwise, only method calling will be
	 * used.
	 * <p>
	 * If a thread is used, {@link WaveMessagingPylonProxy#send(AgentWave)} will always return true.
	 * <p>
	 * <b>WARNING:</b> not using a thread may lead to race conditions and deadlocks. Use only if you know what you are
	 * doing.
	 */
	protected boolean	useThread			= true;
	/**
	 * Indicates whether the messages where the destination is not known should be kept and delivery should be retried.
	 * The value of the parameter indicates the number of retries (see {@link LocalPylon} for more details).
	 */
	protected int		keepUndeliverable	= 0;
	/**
	 * Indicates how often delivery should be retried for messages.
	 */
	protected int		retryEvery			= 5;
	
	/**
	 * The receivers for each agent.
	 */
	protected HashMap<String, WaveReceiver> messageReceivers = new HashMap<>();
	
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this is a reference to that
	 * thread.
	 */
	protected Thread							messageThread	= null;
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this queue is used to gather
	 * messages.
	 */
	protected LinkedBlockingQueue<AgentWave>	messageQueue	= null;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(name == null)
			name = LOCAL_SUPPORT_NAME;
		if(configuration.isSimple(USE_THREAD_PARAM_NAME)
				&& configuration.getAValue(USE_THREAD_PARAM_NAME) == Boolean.FALSE.toString())
			useThread = false;
		if(configuration.isSimple(KEEP_UNDELIVERABLE_PARAM_NAME))
			try {
				keepUndeliverable = Integer.parseInt(configuration.getAValue(KEEP_UNDELIVERABLE_PARAM_NAME));
			} catch(NumberFormatException e) {
				le("Parameter [] is not a number:", KEEP_UNDELIVERABLE_PARAM_NAME,
						configuration.getAValue(KEEP_UNDELIVERABLE_PARAM_NAME));
			}
		if(configuration.isSimple(RETRY_EVERY_PARAM_NAME))
			try {
				retryEvery = Integer.parseInt(configuration.getAValue(RETRY_EVERY_PARAM_NAME));
			} catch(NumberFormatException e) {
				le("Parameter [] is not a number:", RETRY_EVERY_PARAM_NAME,
						configuration.getAValue(RETRY_EVERY_PARAM_NAME));
			}
		messageQueue = new LinkedBlockingQueue<>();
		return true;
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		if(useThread) {
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
			if(messageThread != null)
				try {
					messageThread.join();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			messageQueue = null;
			messageThread = null;
		}
		li("Stopped");
		return true;
	}
	
	@Override
	public void run() {
		processQueue();
	}
	
	/**
	 * Registered that a message must be sent. If a thread and a queue are not used, this method directly calls
	 * {@link #deliver(AgentWave)}.
	 *
	 * @param wave
	 *            - the wave
	 * @return <code>true</code> if the message was sent successfully or if a thread is used.
	 */
	protected boolean send(AgentWave wave) {
		if(useThread) {
			synchronized(messageQueue) {
				messageQueue.add(wave);
				messageQueue.notify();
			}
			return true;
		}
		return deliver(wave);
	}
	
	/**
	 * Attempts the actual delivery of a message.
	 *
	 * @param wave
	 *            - the wave
	 * @return <code>true</code> if the destination is registered, <code>false</code> otherwise.
	 */
	protected boolean deliver(AgentWave wave) {
		String agentName = wave.getFirstDestinationElement();
		if(!messageReceivers.containsKey(agentName)) {
			le("Unable to find destination [] for wave [].", agentName, wave);
			// TODO manage undeliverables
			return false;
		}
		messageReceivers.get(agentName).receive(wave);
		return true;
	}
	
	/**
	 * When using a thread, process (or wait for) messages in the message queue.
	 */
	protected void processQueue() {
		while(useThread) {
			// lf("Messages: ", Integer.valueOf(messageQueue.size()));
			if(messageQueue.isEmpty())
				try {
					synchronized(messageQueue) {
						messageQueue.wait(PlatformUtils.GLOBAL_WAITING_TIME);
					}
				} catch(InterruptedException e) {
					// do nothing
				}
			else {
				AgentWave wave = messageQueue.poll();
				deliver(wave);
			}
		}
	}
	
	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardName) {
		if(shardName.equals(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING)))
			return SimpleLocalMessaging.class.getName();
		return super.getRecommendedShardImplementation(shardName);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Pylon> asContext() {
		return messagingProxy;
	}
}
