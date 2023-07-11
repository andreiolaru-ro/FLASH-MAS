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
package net.xqhs.flash.ros;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.databind.JsonNode;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.NameBasedMessagingShard;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ros.rosBridge.Publisher;
import net.xqhs.flash.ros.rosBridge.RosBridge;
import net.xqhs.flash.ros.rosBridge.RosListenDelegate;
import net.xqhs.flash.ros.rosBridge.SubscriptionRequestMsg;
import net.xqhs.flash.ros.rosBridge.msgs.std_msgs.PrimitiveMsg;
import net.xqhs.flash.ros.rosBridge.tools.MessageUnpacker;

/**
 * Simple support implementation that allows agents to send messages locally (inside the same JVM) based simply on agent
 * name.
 *
 * @author Andrei Olaru
 */
public class RosPylon extends DefaultPylonImplementation {
    /**
     * The type of this support infrastructure (its 'kind')
     */
	public static final String	LOCAL_SUPPORT_NAME	= "ROS";

	public static final String	CONNECTTO_ATTRIBUTE_NAME	= "connect-to";

    RosBridge bridge;

    /**
     * The proxy to this entity.
     */
    public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {
        /**
		 * @param source
		 *            - the source endpoint.
		 * @param destination
		 *            - the destination endpoint.
		 * @param content
		 *            - the content of the message.
		 *			
		 *            New publisher instance. Publishes to topic with name: /agentName, where agentName is destination
		 *            agent's name. The message sent is formatted as follows: source: @param source destination: @param
		 *            destination content: @param content.
		 *			
		 * @return always returns <code>true</code>.
		 */
        @Override
        public boolean send(String source, String destination, String content) {
            String agentName = destination.split(AgentWave.ADDRESS_SEPARATOR)[0];
            Publisher pub = new Publisher("/" + agentName, "std_msgs/String", bridge);
            String formatMessage = "source: " + source;
            formatMessage += " destination: " + destination;
            formatMessage += " content: " + content;
			pub.publish(new PrimitiveMsg<>(formatMessage));
            return true;
        }

        /**
		 * @param agentName
		 *            - the name of the agent.
		 * @param receiver
		 *            - the {@link MessageReceiver} instance to receive messages.
		 *			
		 *            New subscriber to topic with name: /@param agentName. The message received is formatted as
		 *            follows: source: @param source destination: @param destination content: @param content.
		 *			
		 * @return always returns <code>true</code>.
		 */
        @Override
        public boolean register(String agentName, MessageReceiver receiver) {
            messageReceivers.put(agentName, receiver);
            // topic name: id = agentName
            bridge.subscribe(SubscriptionRequestMsg.generate("/" + agentName)
                            .setType("std_msgs/String"),
                    new RosListenDelegate() {
						@Override
						public void receive(JsonNode data, String stringRep) {
							MessageUnpacker<PrimitiveMsg<String>> unpacker = new MessageUnpacker<>(PrimitiveMsg.class);
                            PrimitiveMsg<String> msg = unpacker.unpackRosMessage(data);
                            String[] parsedData = msg.data.split(" ");
                            String receiverSource = parsedData[1];
                            String receiverDestination = parsedData[3];
                            String[] receiverContentList = msg.data.split(": ");
                            String receiverContent = receiverContentList[3];
                            receiver.receive(receiverSource, receiverDestination, receiverContent);
                        }
                    }
            );
            return true;
        }

        @Override
        public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
            return RosPylon.this.getRecommendedShardImplementation(shardType);
        }

        @Override
        public String getEntityName() {
            return getName();
        }
																
																@Override
																public boolean unregister(String entityName,
																		MessageReceiver registeredReceiver) {
																	// TODO Auto-generated method stub
																	return false;
																}
    };

    /**
	 * Simple implementation of {@link AbstractMessagingShard}, that uses agents' names as their addresses.
	 *
	 * @author Andrei Olaru
	 */
	public static class SimpleLocalMessaging extends NameBasedMessagingShard {
		/**
		 * The serial UID.
		 */
		private static final long serialVersionUID = 1L;
		
		@Override
		protected void receiveMessage(String source, String destination, String content) {
			super.receiveMessage(source, destination, content);
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
     * The receivers for each agent.
     */
    protected HashMap<String, MessageReceiver>										messageReceivers	= new HashMap<>();
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
	public boolean configure(MultiTreeMap configuration)
	{
		if(!super.configure(configuration))
			return false;
		if(!configuration.isSimple(CONNECTTO_ATTRIBUTE_NAME))
		{
			System.out.println("No URI specified");
			return false;
		}
		bridge = new RosBridge();
		bridge.connect(configuration.getAValue(CONNECTTO_ATTRIBUTE_NAME), true);
		System.out.println("Connected.");
		return true;
	}
	
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
