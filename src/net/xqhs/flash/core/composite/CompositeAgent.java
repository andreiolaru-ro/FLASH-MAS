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
package net.xqhs.flash.core.composite;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentEvent.AgentSequenceType;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.UnitComponent;

/**
 * This class implements an agent formed by shards and an event queue that allows shards to communicate among each
 * other.
 * <p>
 * Various agent shards -- instances of {@link AgentShard} -- can be added. Shards are identified by means of their
 * designation. At most one shard with the same designation is allowed (i.e. at most one shard per functionality). TODO
 * <p>
 * It is this class that handles agent events, by means of the <code>postAgentEvent()</code> method, which disseminates
 * an event to all shards.
 *
 * @author Andrei Olaru
 */
public class CompositeAgent implements CompositeAgentModel
{
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -5676876894024151157L;
	
	/**
	 * The implementation of {@link ShardContainer} as a proxy for {@link CompositeAgent}.
	 */
	protected class CompositeAgentShardContainer implements ShardContainer, Serializable
	{
		/**
		 * The serial UID.
		 */
		private static final long	serialVersionUID	= 4212641806365747549L;
		/**
		 * The agent
		 */
		CompositeAgent agent;

		/**
		 * @param agent
		 *                  - the agent
		 */
		public CompositeAgentShardContainer(CompositeAgent agent)
		{
			this.agent = agent;
		}

		@Override
		public void postAgentEvent(AgentEvent event)
		{
			agent.postAgentEvent(event);
		}

		@Override
		public String getEntityName()
		{
			return agent.getName();
		}

		@Override
		public AgentShard getAgentShard(AgentShardDesignation designation)
		{
//			System.out.println("getagentshard from " + this + " is " + shards.get(designation));
//			System.out.println("getagentshard shards object " + shards);
			return shards.get(designation);
		}
	}



	/**
	 * Values indicating the current state of the agent, especially with respect to processing events.
	 * <p>
	 * The normal transition between states is the following: <br/>
	 * <ul>
	 * <li>{@link #STOPPED} [here shards are normally added] + {@link AgentEventType#AGENT_START} &rarr;
	 * {@link #STARTING} [starting thread; starting shards] &rarr; {@link #RUNNING}.
	 * <li>while in {@link #RUNNING}, shards can be added or removed.
	 * <li>{@link #RUNNING} + {@link AgentEventType#AGENT_STOP} &rarr; {@link #STOPPING} [no more events accepted; stop
	 * shards; stop thread] &rarr; {@link #STOPPED}.
	 * <li>when the {@link #TRANSIENT} state is involved, the transitions are as follows: {@link #RUNNING} +
	 * {@link AgentEventType#AGENT_STOP} w/ parameter {@link CompositeAgent#TRANSIENT_EVENT_PARAMETER} &rarr;
	 * {@link #STOPPING} &rarr {@link #TRANSIENT} [unable to modify agent] + {@link AgentEventType#AGENT_START} w/
	 * parameter {@link CompositeAgent#TRANSIENT_EVENT_PARAMETER} &rarr; {@link #RUNNING}.
	 * </ul>
	 *
	 * @author Andrei Olaru
	 */
	enum AgentState {
		/**
		 * State indicating that the agent is currently behaving normally and agent events are processed in good order.
		 * All shards are running.
		 */
		RUNNING,

		/**
		 * State indicating that the agent is stopped and is unable to process events. The agent's thread is stopped.
		 * All shards are stopped.
		 */
		STOPPED,

		/**
		 * This state is a version of the {@link #STOPPED} state, with the exception that it does not allow any changes
		 * the general state of the agent (e.g. shard list). The state should be used to "freeze" the agent, such as for
		 * it to be serialized. Normally, in this state shards should not allow any changes either.
		 */
		TRANSIENT,

		/**
		 * State indicating that the agent is in the process of starting, but is not currently accepting events. The
		 * thread may or may not have been started. The shards are in the process of starting.
		 */
		STARTING,

		/**
		 * State indicating that the agent is currently stopping. It is not accepting events anymore. The thread may or
		 * may not be running. The shards are in the process of stopping.
		 */
		STOPPING,
	}

	/**
	 * This is the event-processing thread of the agent.
	 *
	 * @author Andrei Olaru
	 */
	class AgentThread implements Runnable
	{
		@Override
		public void run()
		{
			eventProcessingCycle();
		}
	}

	/**
	 * The name of the parameter that should be added to {@link AgentEventType#AGENT_START} /
	 * {@link AgentEventType#AGENT_STOP} events in order to take the agent out of / into the <code>TRANSIENT</code>
	 * state.
	 */
	public static final String								TRANSIENT_EVENT_PARAMETER	= "TO_FROM_TRANSIENT";
	/**
	 * The name of the parameter that should be added to {@link AgentEventType#AGENT_START} in order to signal that a
	 * separate {@link AgentThread} instance should not be created.
	 */
	protected static final String							NO_CREATE_THREAD			= "DONT_CREATE_THREAD";

	/**
	 * This can be used by support implementation-specific shards to contact the support implementation.
	 */
	protected EntityProxy<Pylon>							supportLink					= null;

	/**
	 * The proxy to this agent.
	 */
	protected EntityProxy<Agent>							asContext					= new CompositeAgentShardContainer(
			this);

	/**
	 * The {@link Map} that links shard designations (functionalities) to shard instances.
	 * FIXME: support making shards transient and having shards null
	 */
	public Map<AgentShardDesignation, AgentShard>		shards						= new HashMap<>(); // change to protected
	/**
	 * A {@link List} that holds the order in which shards were added, to signal agent events to shards in the
	 * correct order (as specified by {@link AgentSequenceType}).
	 * <p>
	 * It is important that this list is managed together with {@link #shards}.
	 */
	protected ArrayList<AgentShardDesignation>				shardOrder		= new ArrayList<>();
	/**
	 * The list of all contexts this agent is placed in, in the order in which they were added.
	 */
	protected ArrayList<EntityProxy<? extends Entity<?>>>	agentContext				= new ArrayList<>();

	/**
	 * A synchronized queue of agent events, as posted by the shards or by the agent itself.
	 */
	protected LinkedBlockingQueue<AgentEvent>				eventQueue					= null;
	/**
	 * The thread managing the agent's life-cycle (managing events).
	 */
	protected transient Thread										agentThread					= null;
	/**
	 * The agent state. See {@link AgentState}. Access to this member should be synchronized with the lock of
	 * <code>eventQueue</code>.
	 */
	protected AgentState									agentState					= AgentState.STOPPED;

	/**
	 * The agent name, if given.
	 */
	protected String										agentName;
	/**
	 * <b>*EXPERIMENTAL*</b>. This log is used only for important logging messages related to the agent's state. While
	 * the agent will attempt to use its set name, this may not always succeed. This log should only be used by means of
	 * the {@link #log(String, Object...)} method.
	 */
	@SuppressWarnings("deprecation")
	protected transient UnitComponent	localLog		= new UnitComponent()
			.setLoggerType(PlatformUtils.platformLogType()).setLogLevel(Level.INFO);
	/**
	 * This switch activates the use of the {@link #localLog}.
	 */
	protected boolean										USE_LOCAL_LOG				= true;

	/**
	 * The default constructor. Should be used only when deserializing.
	 */
	public CompositeAgent() {
	}
	
	/**
	 * Constructor for {@link CompositeAgent} instances.
	 * <p>
	 * The configuration is used to extract the name of the agent from it (as the value associated with the
	 * {@link DeploymentConfiguration#NAME_ATTRIBUTE_NAME} name).
	 * <p>
	 * Although the name may be null, it is strongly recommended that the agent is given a (unique) name, even one that
	 * is automatically generated.
	 *
	 * @param configuration
	 *            - the configuration, from which the name of the agent will be taken.
	 */
	public CompositeAgent(MultiTreeMap configuration)
	{
		if(configuration != null && configuration.containsKey(DeploymentConfiguration.NAME_ATTRIBUTE_NAME))
			agentName = configuration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
	}

	/**
	 * Starts the life-cycle of the agent. All shards will receive an {@link AgentEventType#AGENT_START} event.
	 *
	 * @return true if the event has been successfully posted. See <code>postAgentEvent()</code>.
	 */
	@Override
	public boolean start()
	{
		return postAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
	}

	@Override
	public void run()
	{
		postAgentEvent((AgentEvent) new AgentEvent(AgentEventType.AGENT_START).add(NO_CREATE_THREAD, NO_CREATE_THREAD));
	}

	/**
	 * Instructs the agent to unload all shards and exit. All shards will receive an {@link AgentEventType#AGENT_STOP}
	 * event.
	 * <p>
	 * No events will be successfully received after this event has been posted.
	 *
	 * @return true if the event has been successfully posted. See <code>postAgentEvent()</code>.
	 */
	public boolean exit()
	{
		return postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
	}

	/**
	 * Alias for {@link #exit()}.
	 */
	@Override
	public boolean stop()
	{
		return exit();
	}

	/**
	 * Instructs the agent to switch state between <code>STOPPED</code> and <code>TRANSIENT</code>.
	 *
	 * @return <code>true</code> if the agent is now in the <code>TRANSIENT</code> state, <code>false</code> otherwise.
	 *
	 * @throws RuntimeException
	 *                              if the agent was in any other state than the two.
	 */
	public boolean toggleTransient() throws RuntimeException
	{
		return FSMToggleTransient();
	}

	/**
	 * The method handles the entire event processing cycle of the agent, from after the
	 * {@link AgentEventType#AGENT_START} event to the {@link AgentEventType#AGENT_STOP} event. The method should only
	 * return when the agent has completed stopping.
	 * 
	 * @return the event that caused the cycle to exit, if any; <code>null</code> otherwise.
	 */
	protected AgentEvent eventProcessingCycle()
	{
		boolean threadExit = false;
		// TODO: should be able to do without threadExit, and with while(true).
		while(!threadExit)
		{
			if(eventQueue == null)
			{
				log("No event queue present");
				return null;
			}
			// System.out.println("oops");
			AgentEvent event = null;
			synchronized(eventQueue)
			{
				if(eventQueue.isEmpty())
					try
					{
						eventQueue.wait();
					} catch(InterruptedException e)
					{
						// do nothing
					}
				if(!eventQueue.isEmpty())
					event = eventQueue.poll();
			}
			if(event != null)
			{
				switch(event.getType().getSequenceType())
				{
				case CONSTRUCTIVE:
				case UNORDERED:
					for(AgentShardDesignation shardDesignation : shardOrder)
						if(shards.containsKey(shardDesignation))
							shards.get(shardDesignation).signalAgentEvent(event);
					break;
				case DESTRUCTIVE:
					for(ListIterator<AgentShardDesignation> it = shardOrder.listIterator(shardOrder.size()); it
							.hasPrevious();) {
						AgentShardDesignation shardDesignation = it.previous();
						if(shards.containsKey(shardDesignation))
							shards.get(shardDesignation).signalAgentEvent(event);
					}
					break;
				default:
					throw new IllegalStateException(
							"Unsupported sequence type: " + event.getType().getSequenceType().toString());
				}

				threadExit = FSMEventOut(event.getType(), event.isSet(TRANSIENT_EVENT_PARAMETER));
				if(threadExit)
					return event;
					
//				if (MOVE_TRANSIENT_EVENT_PARAMETER.equals(event.get(TRANSIENT_EVENT_PARAMETER))) {
//					// serializarea
//
//					String destination = event.getValue("target");
//					String agentData = serialize();
//					JSONObject root = new JSONObject();
//					root.put(OperationUtils.NAME, OperationUtils.ControlOperation.RECEIVE_AGENT.toString().toLowerCase());
//					root.put(OperationUtils.PARAMETERS, destination);
//					root.put("agentData", agentData);
//
//					String json = root.toJSONString();
//
//					Node.NodeProxy nodeProxy = getNodeProxyContext();
//					if (nodeProxy != null) {
//						removeGeneralContext(nodeProxy);
//						nodeProxy.moveAgent(destination, json);
//					}
//				}
			}
		}
		return null;
	}

	/**
	 * The method should be called by an agent shard (relayed through {@link AgentShard}) to disseminate a an
	 * {@link AgentEvent} to the other shards.
	 * <p>
	 * If the event has been successfully posted, the method returns <code>true</code>, guaranteeing that, except in the
	 * case of abnormal termination, the event will be processed eventually. Otherwise, it returns <code>false</code>,
	 * indicating that either the agent has not been started, or has been instructed to exit, or is in another
	 * inappropriate state.
	 *
	 * @param event
	 *                  the event to disseminate.
	 * @return <code>true</code> if the event has been successfully posted; <code>false</code> otherwise.
	 */
	protected boolean postAgentEvent(AgentEvent event)
	{
		// TODO: commented this because agent events may need to be processed further. Think if this is a good idea.
		// event.lock();

		if(!canPostEvent(event))
			return false;

		AgentState futureState = FSMEventIn(event.getType(), event.isSet(TRANSIENT_EVENT_PARAMETER),
				!event.isSet(NO_CREATE_THREAD));

		try
		{
			if(eventQueue != null)
				synchronized(eventQueue)
				{
					if(futureState != null)
						agentState = futureState;
					eventQueue.put(event);
					eventQueue.notify();
					// log("put event", event.getType());
				}
			else
			{
				log("There is no event queue.");
				return false;
			}
		} catch(InterruptedException e)
		{
			e.printStackTrace();
			return false;
		}

//		if (event.getType().equals(AgentEventType.AGENT_WAVE)) {
//			CompositeAgent agent;
//			ByteArrayInputStream fis;
//			ObjectInputStream in;
//			try {
//				fis = new ByteArrayInputStream(Base64.getDecoder().decode(event.get("content")));
//				in = new ObjectInputStream(fis);
//				agent = (CompositeAgent) in.readObject();
//				agent.toggleTransient();
//				agent.start();
//				in.close();
//				System.out.println("Deserialized agent obj from string:");
//				System.out.println(agent);
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}

		if(event.getType().equals(AgentEventType.AGENT_START) && futureState != null
				&& futureState.equals(AgentState.STARTING) && event.isSet(NO_CREATE_THREAD))
			eventProcessingCycle();
		return true;
	}

	/**
	 * Checks whether the specified event can be posted in the current agent state.
	 * <p>
	 * Basically, there are two checks:
	 * <ul>
	 * <li>Any event except {@link AgentEventType#AGENT_START} can be posted only in the {@link AgentState#RUNNING}
	 * state.
	 * <li>If the {@link AgentEventType#AGENT_START} is posted while the agent is in the {@link AgentState#TRANSIENT}
	 * state, it needs to shard a parameter called {@value #TRANSIENT_EVENT_PARAMETER} (with any value).
	 * <li>The {@link AgentEventType#AGENT_START} event can be posted while the agent is {@link AgentState#STOPPED}.
	 *
	 * @param event
	 *                  - the event one desires to post.
	 * @return <code>true</code> if the event could be posted at this moment; <code>false</code> otherwise.
	 */
	protected boolean canPostEvent(AgentEvent event)
	{
		switch(event.getType())
		{
		case AGENT_START:
			if(agentState == AgentState.TRANSIENT)
				return event.isSet(TRANSIENT_EVENT_PARAMETER);
			return agentState == AgentState.STOPPED;
		default:
			return agentState == AgentState.RUNNING;
		}
	}

	/**
	 * Change the state of the agent (if it is the case) and perform other actions, <i>before</i> an event is added to
	 * the event queue. It is presumed that the event has already been checked with {@link #canPostEvent(AgentEvent)}
	 * and that the event can indeed be posted to the queue in the current state.
	 * <p>
	 * If the event was {@link AgentEventType#AGENT_START}, the agent will enter {@link AgentState#STARTING}, the event
	 * queue is created and the agent thread is started. This method will complete only after the agent thread is
	 * actually started (synchronization is done through {@link #eventQueue}).
	 * <p>
	 * If the event was {@link AgentEventType#AGENT_STOP}, the agent will enter {@link AgentState#STOPPING}.
	 *
	 * @param eventType
	 *                            - the type of the event.
	 * @param fromToTransient
	 *                            - <code>true</code> if the agent should enter / exit from the
	 *                            {@link AgentState#TRANSIENT} state.
	 * @param createThread
	 *                            - <code>true</code> if a thread should be created to process events in the event
	 *                            queue; <code>false</code> if this method should only return when the agent has
	 *                            stopped.
	 * @return the state the agent should enter next (the actual state change will happen in
	 *         {@link #postAgentEvent(AgentEvent)}, together with posting the event to the queue).
	 */
	protected AgentState FSMEventIn(AgentEventType eventType, boolean fromToTransient, boolean createThread)
	{
		AgentState futureState = null;
		switch(eventType)
		{
		case AGENT_START:
			futureState = AgentState.STARTING;

			if(eventQueue != null)
				log("event queue already present");
			eventQueue = new LinkedBlockingQueue<>();
			if(createThread)
			{
				agentThread = new Thread(new AgentThread());
				agentThread.start();
			}
			else
			{
				agentThread = null;
			}
			break;
		case AGENT_STOP:
			futureState = AgentState.STOPPING;
			break;
		default:
			// nothing to do
		}
		if(futureState != null)
			log("Agent state is soon [][]", futureState, fromToTransient ? "transient" : "");
		return futureState;
	}

	/**
	 * Change the state of the agent (if it is the case) and perform other actions, <i>after</i> an event has been
	 * processed by all shards.
	 * <p>
	 * If the event was {@link AgentEventType#AGENT_START}, the state will be {@link AgentState#RUNNING}.
	 * <p>
	 * If the event was {@link AgentEventType#AGENT_STOP}, the event queue will be consumed, the state will be
	 * {@link AgentState#STOPPED} or {@link AgentState#TRANSIENT} (depending on the event parameters), and the log and
	 * thread will exit.
	 *
	 * @param eventType
	 *                            - the type of the event.
	 * @param toFromTransient
	 *                            - <code>true</code> if the agent should enter / exit from the
	 *                            {@link AgentState#TRANSIENT} state.
	 * @return <code>true</code> if the agent thread should exit.
	 */
	protected boolean FSMEventOut(AgentEventType eventType, boolean toFromTransient)
	{
		switch(eventType)
		{
		case AGENT_START: // the agent has completed starting and all shards are up.
			synchronized(eventQueue)
			{
				agentState = AgentState.RUNNING;
				log("state is now ", agentState);
			}
			break;
		case AGENT_STOP:
			synchronized(eventQueue)
			{
				if(!eventQueue.isEmpty())
				{
					while(!eventQueue.isEmpty())
						log("ignoring event ", eventQueue.poll());
				}
				if(toFromTransient)
					agentState = AgentState.TRANSIENT;
				else
					agentState = AgentState.STOPPED;
				log("state is now ", agentState);
			}
			eventQueue = null;
			localLog.doExit();
			return true;
		default:
			// do nothing
		}
		return false;
	}

	/**
	 * Changes the agent state between {@link AgentState#STOPPED} and {@link AgentState#TRANSIENT}. If the agent is in
	 * any other state, an exception is thrown.
	 *
	 * @return <code>true</code> if the agent is now (after the change) in the {@link AgentState#TRANSIENT} state.
	 *         <code>false</code> if it is now in {@link AgentState#STOPPED}.
	 *
	 * @throws RuntimeException
	 *                              if the agent is in any other state than the two above.
	 */
	protected boolean FSMToggleTransient() throws RuntimeException
	{
		switch(agentState)
		{
		case STOPPED:
			agentState = AgentState.TRANSIENT;
			break;
		case TRANSIENT:
			agentState = AgentState.STOPPED;
			break;
		default:
			throw new IllegalStateException("Unable to toggle TRANSIENT state while in " + agentState);
		}
		if(localLog.getUnitName() != null)
			// protect against locking the log
			log("state switched to ", agentState);
		return isTransient();
	}

	/**
	 * Context can be added to an agent only when it is not running.
	 */
	@Override
	public boolean addContext(EntityProxy<Pylon> context)
	{
		return addGeneralContext(context);
	}

	/**
	 * Context can be removed from an agent only when it is not running.
	 */
	@Override
	public boolean removeContext(EntityProxy<Pylon> context)
	{
		return removeGeneralContext(context);
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		if(isRunning()) {
			return false;
		}

		agentContext.add(context);
		System.out.println("adaug pylon in shard din agent");
		for(AgentShard shard : shards.values()) {
			System.out.println("adaug pylon in shard " + shard + " " + this.getName());
			shard.addGeneralContext(context);
		}

		return true;
	}

	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		if(isRunning())
			return false;
		agentContext.remove(context);
		for(AgentShard shard : shards.values())
			shard.removeGeneralContext(context);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Agent> asContext()
	{
		return asContext;
	}

	/**
	 * Adds a shard to the agent, which has been configured beforehand. The agent will register with the shard, as
	 * parent.
	 * <p>
	 * The shard will be identified by the agent by means of its {@link AgentShard#getShardDesignation()} method. Only
	 * one instance per designation (functionality) will be allowed.
	 *
	 * @param shard
	 *                  - the {@link AgentShard} instance to add.
	 * @return the agent instance itself. This can be used to continue adding other shards.
	 */
	@Override
	public CompositeAgent addShard(AgentShard shard)
	{
		if(!canAddShards())
			throw new IllegalStateException("Cannot add shards in state [" + agentState + "].");
		if(shard == null)
			throw new InvalidParameterException("Shard is null");
		if(hasShard(shard.getShardDesignation()))
			throw new InvalidParameterException(
					"Cannot add multiple shards for designation [" + shard.getShardDesignation() + "]");

//		if (shard.getShardDesignation().toString().equalsIgnoreCase("messaging")) {
//			System.out.println("aa adaug messaging shard " + shard);
//		}
		System.out.println("Shards before addShard " + shards);
		shards.put(shard.getShardDesignation(), shard);
		shardOrder.add(shard.getShardDesignation());
		shard.addContext(this.asContext());
		for(EntityProxy<? extends Entity<?>> context : agentContext)
			shard.addGeneralContext(context);
		System.out.println("Shards after addShard " + shards);
		return this;
	}

	/**
	 * Removes an existing shard of the agent.
	 *
	 * @param designation
	 *                        - the designation of the shard to remove.
	 * @return a reference to the just-removed shard instance.
	 */
	protected AgentShard removeShard(AgentShardDesignation designation)
	{
		if(!hasShard(designation))
			throw new InvalidParameterException("Shard [" + designation + "] does not exist");
		AgentShard shard = getShard(designation);
		shardOrder.remove(designation);
		shards.remove(designation);
		return shard;
	}

	/**
	 * Returns <code>true</code> if the agent contains said shard.
	 *
	 * @param designation
	 *                        - the designation of the shard to search.
	 * @return <code>true</code> if the shard exists, <code>false</code> otherwise.
	 */
	protected boolean hasShard(AgentShardDesignation designation)
	{
		return shards.containsKey(designation);
	}

	/**
	 * Retrieves a shard of the agent, by designation.
	 * <p>
	 * It is <i>strongly recommended</i> that the reference is not kept, as the shard may be removed without notice.
	 *
	 * @param designation
	 *                        - the designation of the shard to retrieve.
	 * @return the {@link AgentShard} instance, if any. <code>null</code> otherwise.
	 */
	protected AgentShard getShard(AgentShardDesignation designation)
	{
		return shards.get(designation);
	}

	/**
	 * Retrieves the link to the support implementation.
	 *
	 * @return the support implementation.
	 */
	protected Object getSupportImplementation()
	{
		return supportLink;
	}

	/**
	 * Returns the name of the agent. It is the name that has been set through the <code>AGENT_NAME</code> parameter.
	 *
	 * @return the name of the agent.
	 */
	@Override
	public String getName()
	{
		return agentName;
	}

	/**
	 * Checks if the agent is currently in <code>RUNNING</code> state. In case shards are added during this state, they
	 * must consider that the agent is already running and no additional {@link AgentEventType#AGENT_START} events will
	 * be issued.
	 *
	 * @return <code>true</code> if the agent is currently <code>RUNNING</code>; <code>false</code> otherwise.
	 */
	@Override
	public boolean isRunning()
	{
		return agentState == AgentState.RUNNING;
	}

	/**
	 * Checks if the agent is currently in <code>STOPPED</code> state.
	 *
	 * @return <code>true</code> if the agent is currently <code>STOPPED</code>; <code>false</code> otherwise.
	 */
	public boolean isStopped()
	{
		return agentState == AgentState.STOPPED;
	}

	/**
	 * Checks whether the agent is in the <code>TRANSIENT</code> state.
	 *
	 * @return <code>true</code> if the agent is currently <code>TRANSIENT</code>; <code>false</code> otherwise.
	 */
	public boolean isTransient()
	{
		return agentState == AgentState.TRANSIENT;
	}

	/**
	 * Checks whether the agent is in the <code>STARTING</code> state.
	 *
	 * @return <code>true</code> if the agent is currently <code>STARTING</code>; <code>false</code> otherwise.
	 */
	protected boolean isStarting()
	{
		return agentState == AgentState.STARTING;
	}

	/**
	 * Checks whether the agent is in the <code>STOPPING</code> state.
	 *
	 * @return <code>true</code> if the agent is currently <code>STOPPING</code>; <code>false</code> otherwise.
	 */
	protected boolean isStopping()
	{
		return agentState == AgentState.STOPPING;
	}

	/**
	 * Checks if the state of the agent allows adding shards. Shards should not be added in intermediary states in which
	 * the agent is starting or stopping.
	 *
	 * @return <code>true</code> if in the current state shards can be added.
	 */
	public boolean canAddShards()
	{
		return (agentState == AgentState.STOPPED) || (agentState == AgentState.TRANSIENT)
				|| (agentState == AgentState.RUNNING);
	}

	/**
	 * Returns the name of the agent.
	 */
	@Override
	public String toString()
	{
		return getName();
	}

	/**
	 * Use this method to output to the local log. Do not abuse. The call is relayed to a
	 * {@link UnitComponent#li(String, Object...)} call.
	 *
	 * @param message
	 *                      - the message.
	 * @param arguments
	 *                      - objects to include in the message.
	 */
	protected void log(String message, Object... arguments)
	{
		if(USE_LOCAL_LOG && (localLog != null))
		{
			if(localLog.getUnitName() == null)
			{
				if(getName() != null)
					localLog.setUnitName(getName() + "#");
				else
					localLog.setUnitName(super.toString().replace(getClass().getName(), "CompAg") + "#");
			}
			localLog.li(message, arguments);
		}
	}
}
