package net.xqhs.flash.core.mobileComposite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.composite.CompositeAgentLoader;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.Node.NodeProxy;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.UnitComponent;

/**
 * This class extends {@link CompositeAgent} and is specifically destined to moving agents.
 *
 * @author Maria-Claudia Buiac
 * @author Andrei Olaru
 */
public class MobileCompositeAgent extends CompositeAgent {
	
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -308343471716425142L;
	
	/**
	 * The name of the parameter that should be added to {@link AgentEventType#AGENT_START} /
	 * {@link AgentEventType#AGENT_STOP} events in order to signal that the agent has moved.
	 */
	public static final String MOVE_TRANSIENT_EVENT_PARAMETER = "MOVE";
	
	/**
	 * Key corresponding to the destination of a moving agent, in a pair that should be added to an
	 * {@link AgentEventType#AGENT_STOP} event.
	 */
	public static final String TARGET = "TARGET";
	
	/**
	 * Map with {@link AgentShardDesignation}/Serialization of shards, for reloading them after moving
	 */
	public Map<AgentShardDesignation, String> serializedShards = new HashMap<>();
	
	/**
	 * Map with {@link AgentShardDesignation}/{@link MultiTreeMap} - representing shard configurations, for reloading
	 * shards that cannot be serialized.
	 */
	public Map<AgentShardDesignation, MultiTreeMap> nonSerializedShardDesignations = new HashMap<>();
	
	/**
	 * Loader for loading non-serializable shards.
	 */
	private transient CompositeAgentLoader loader = new CompositeAgentLoader();
	
	/**
	 * The implementation of {@link ShardContainer} as a proxy for {@link MobileCompositeAgent}.
	 */
	public class MobileCompositeAgentShardContainer extends CompositeAgentShardContainer {
		/**
		 * The serial UID.
		 */
		private static final long	serialVersionUID	= 4212641806365747549L;
		
		/**
		 * @param agent
		 *            - the agent
		 */
		public MobileCompositeAgentShardContainer(MobileCompositeAgent agent) {
			super(agent);
		}
		
		/**
		 * Starts the mobility process
		 * 
		 * @param destination
		 *            - the destination of the movement.
		 * @return <code>true</code> if initiating the migration proceeded correctly and the agent is expected to move
		 *         at some point in the near future. <code>false</code> if the agent will not move as an effect of this
		 *         call.
		 */
		public boolean moveTo(String destination) {
			return ((MobileCompositeAgent) agent).moveTo(destination);
		}
		
		/**
		 * @return the name of the {@link Node} that this agent is currently in the context of.
		 */
		public String getCurrentNode() {
			NodeProxy nodeProxy = getNodeProxyContext();
			return nodeProxy != null ? nodeProxy.getEntityName() : null;
		}
	}
	
	/**
	 * No-argument constructor. This <b>should only be used</b> at de-serialization.
	 */
	public MobileCompositeAgent() {
		asContext = new MobileCompositeAgentShardContainer(this);
	}
	
	/**
	 * Constructor with configuration.
	 * 
	 * @param configuration
	 */
	public MobileCompositeAgent(MultiTreeMap configuration) {
		super(configuration);
		asContext = new MobileCompositeAgentShardContainer(this);
	}
	
	/**
	 * De-serializes an agent instance. No other operations are performed (such as loading shards, etc.). All other
	 * operations are done when the agent is started, after context has been added.
	 * 
	 * @param agentData
	 *            - serialized agent in String form.
	 * @return the de-serialized {@link MobileCompositeAgent} instance.
	 */
	public static MobileCompositeAgent deserializeAgent(String agentData) {
		MobileCompositeAgent agent = null;
		ByteArrayInputStream fis;
		ObjectInputStream in;
		try {
			fis = new ByteArrayInputStream(Base64.getDecoder().decode(agentData));
			in = new ObjectInputStream(fis);
			agent = (MobileCompositeAgent) in.readObject();
			in.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return agent;
	}
	
	@Override
	public boolean start() {
		if(agentState != AgentState.TRANSIENT)
			return super.start();
		
		// this is the point where the agent has arrived after mobility.
		loadShards();
		
		log("agent has moved successfully");
		if(!postAgentEvent((AgentEvent) new AgentEvent(AgentEvent.AgentEventType.AGENT_START)
				.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER)))
			return false;
		AgentShard msgShard = getShard(StandardAgentShard.MESSAGING.toAgentShardDesignation());
		if(msgShard == null || !(msgShard instanceof MobilityAwareMessagingShard))
			return postAgentEvent(new AgentEvent(AgentEventType.AFTER_MOVE));
		return true;
	}
	
	/**
	 * Loads shards after moving.
	 */
	public void loadShards() {
		localLog = new UnitComponent(getName() + "~").setLoggerType(PlatformUtils.platformLogType())
				.setLogLevel(Level.ALL);
		loader = new CompositeAgentLoader();
		loader.configure(new MultiTreeMap(), localLog, PlatformUtils.getClassFactory());
		
		serializedShards.forEach((designation, serializedShard) -> addShard(deserializeShard(serializedShard)));
		
		nonSerializedShardDesignations.forEach((designation, configuration) -> {
			loader.preloadShard(designation.toString(), configuration, null, "PRE_LOADING_NON-SERIALIZED_SHARDS: ");
			AgentShard shard = loader.loadShard(designation.toString(), configuration,
					"LOADING_NON-SERIALIZED_SHARDS: ", agentName);
			addShard(shard);
		});
		
		log("agent [] has shards [] after deserialization in order: []", agentName, shards, shardOrder);
	}
	
	/**
	 * De-serializes a shard.
	 * 
	 * @param serializedShard
	 *            - serialized shard in {@link String} form.
	 * @return the de-serialized {@link AgentShard}.
	 */
	@SuppressWarnings("static-method")
	protected AgentShard deserializeShard(String serializedShard) {
		AgentShard agentShard = null;
		ByteArrayInputStream fis;
		ObjectInputStream in;
		try {
			fis = new ByteArrayInputStream(Base64.getDecoder().decode(serializedShard));
			in = new ObjectInputStream(fis);
			agentShard = (AgentShard) in.readObject();
			in.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return agentShard;
	}
	
	/**
	 * Overrides the implementation in {@link CompositeAgent}. When an event with a
	 * {@link MobileCompositeAgent#MOVE_TRANSIENT_EVENT_PARAMETER} is received, the agent is packaged in a JSON with the
	 * destination and sent to the node proxy.
	 */
	@Override
	protected AgentEvent eventProcessingCycle() {
		AgentEvent exitEvent = super.eventProcessingCycle();
		
		if(exitEvent != null && MOVE_TRANSIENT_EVENT_PARAMETER.equals(exitEvent.get(TRANSIENT_EVENT_PARAMETER))) {
			Node.NodeProxy nodeProxy = getNodeProxyContext();
			if(nodeProxy != null) {
				List<EntityProxy<? extends Entity<?>>> contexts = new ArrayList<>(agentContext);
				contexts.forEach(this::removeGeneralContext);
				nodeProxy.moveAgent(exitEvent.getValue(TARGET), agentName, serialize());
			}
		}
		return exitEvent;
	}
	
	/**
	 * Starts the mobility process. It posts an {@link AgentEventType#AGENT_STOP} event and then either expects the
	 * {@link MobilityAwareMessagingShard} to issue the stopping event or posts an {@link AgentEventType#AGENT_STOP}
	 * with the indication that the agent should become transient.
	 * 
	 * @param destination
	 *            - the destination of the movement.
	 * @return <code>true</code> if initiating the migration proceeded correctly and the agent is expected to move at
	 *         some point in the near future. <code>false</code> if the agent will not move as an effect of this call.
	 */
	protected boolean moveTo(String destination) {
		log("preparing to move to []", destination);
		if(!postAgentEvent((AgentEvent) new AgentEvent(AgentEventType.BEFORE_MOVE).add(TARGET, destination)))
			return false;
		AgentShard msgShard = getShard(StandardAgentShard.MESSAGING.toAgentShardDesignation());
		if(msgShard == null || !(msgShard instanceof MobilityAwareMessagingShard))
			return postAgentEvent((AgentEvent) new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP)
					.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER).add(TARGET, destination));
		return true;
	}
	
	/**
	 * Serializes the current agent after:
	 * <ul>
	 * <li>sorting the shards between serializable and non-serializable
	 * <li>emptying shards map.
	 * </ul>
	 * 
	 * <code>shardOrder</code> is kept in order to keep the same order.
	 * 
	 * @return The serialized form for the agent.
	 */
	public String serialize() {
		log("Serializing shards [] with the order [].", shards, shardOrder);
		for(AgentShardDesignation designation : shards.keySet()) {
			AgentShard shard = shards.get(designation);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
				objectOutputStream.writeObject(shard);
				
				log("[] is serializable", shard);
				serializedShards.put(designation,
						Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
			} catch(NotSerializableException e) {
				MultiTreeMap configuration = null;
				if(shard instanceof NonSerializableShard) {
					log("[] is instance of NonSerializableShard", shard);
					configuration = ((NonSerializableShard) shard).getShardConfiguration();
				}
				else if(shard instanceof AgentShardCore) {
					log("[] is instance of AgentShardCore", shard);
					configuration = ((AgentShardCore) shard).getShardConfiguration();
				}
				else
					log("[] only designation available for shard", shard);
				nonSerializedShardDesignations.put(designation, configuration);
				
			} catch(IOException e) {
				log("Unable to do anything with shard [].", designation);
			}
		}
		
		// the shards map will be recreated at de-serialization.
		shards.clear();
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream;
		try {
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(this);
			objectOutputStream.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
	}
	
	/**
	 * @return the proxy to the current node, if any; <code>null</code> otherwise.
	 */
	public NodeProxy getNodeProxyContext() {
		for(EntityProxy<? extends Entity<?>> context : agentContext)
			if(context instanceof NodeProxy)
				return (NodeProxy) context;
		return null;
	}
	
	@Override
	protected void log(String message, Object... arguments) {
		super.log(message, arguments);
	}
}
