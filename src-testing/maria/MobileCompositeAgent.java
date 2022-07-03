package maria;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.composite.CompositeAgentLoader;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.UnitComponent;


/**
 * This class extends {@link CompositeAgent} and is specifically destined to moving agents.
 *
 * @author Maria Buiac
 */
public class MobileCompositeAgent extends CompositeAgent implements Serializable {

	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -308343471716425142L;

	/**
	 * The name of the parameter that should be added to {@link AgentEvent.AgentEventType#AGENT_START} /
	 * {@link AgentEvent.AgentEventType#AGENT_STOP} events in order to signal that the agent has moved.
	 */
	public static final String MOVE_TRANSIENT_EVENT_PARAMETER = "MOVE";

	/**
	 * Key corresponding to the destination of a moving agent, in a pair that should be added to an
	 * {@link AgentEvent.AgentEventType#AGENT_STOP} event.
	 */
    public static final String TARGET = "TARGET";

//	/**
//	 * The proxy to this agent.
//	 */
//	protected EntityProxy<Agent> asContext = new MobileCompositeAgentShardContainer(this);

	/**
	 * Map with {@link AgentShardDesignation}/Serialization of shards, for reloading them after moving
	 */
    public Map<AgentShardDesignation, String> serializedShards = new HashMap<>();

	/**
	 * Map with {@link AgentShardDesignation}/{@link MultiTreeMap} - representing shard configurations,
	 * for reloading shards that cannot be serialized.
	 */
	public Map<AgentShardDesignation, MultiTreeMap> nonSerializedShardDesignations = new HashMap<>();

	/**
	 * Loader for loading non-serializable shards.
	 */
	private transient CompositeAgentLoader loader = new CompositeAgentLoader();

	private Timestamp moveTime;

	/**
	 * The implementation of {@link ShardContainer} as a proxy for {@link MobileCompositeAgent}.
	 */
	protected class MobileCompositeAgentShardContainer extends CompositeAgentShardContainer
	{
		/**
		 * The serial UID.
		 */
		private static final long	serialVersionUID	= 4212641806365747549L;
		/**
		 * The agent
		 */
		MobileCompositeAgent agent;

		/**
		 * @param agent
		 *                  - the agent
		 */
		public MobileCompositeAgentShardContainer(MobileCompositeAgent agent)
		{
			super(agent);
		}

		public void moveTo(String destination) {
			log("agent [] has been requested to move to []", agentName, destination);
			log("agent [] started moving process at []", agentName, moveTime);
			log("agent [] has shards: [] before serialization", agentName, shards);
//			System.out.println("### SHARDS BEFORE SERIALIZATION: " + shards);
			AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
			prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
			prepareMoveEvent.add(TARGET, destination);
			postAgentEvent(prepareMoveEvent);
		}
	}

	public MobileCompositeAgent() {
		asContext = new MobileCompositeAgentShardContainer(this);
	}

	public MobileCompositeAgent(MultiTreeMap configuration) {
		super(configuration);
		asContext = new MobileCompositeAgentShardContainer(this);
	}

	/**
	 * Finds the proxy of the current node
	 */
	public Node.NodeProxy getNodeProxyContext()
	{
		for(var context : agentContext)
		{
			if(context instanceof Node.NodeProxy)
			{
				return (Node.NodeProxy) context;
			}
		}
		return null;
	}

	/**
	 * @param destination - name of the node where the agent should move
	 */
    public void moveTo(String destination) {
		moveTime = new Timestamp(System.currentTimeMillis());
		log("agent [] has been requested to move to []", agentName, destination);
		log("agent [] started moving at []", agentName, moveTime);
		log("agent [] has shards: [] before serialization", agentName, shards);
//			System.out.println("### SHARDS BEFORE SERIALIZATION: " + shards);
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
        prepareMoveEvent.add(TARGET, destination);
        postAgentEvent(prepareMoveEvent);
    }

	@Override
	public boolean start() {
		if(agentState != AgentState.TRANSIENT)
			return super.start();
		
        // adding transient event parameter
		loadShards();

		log("agent [] has moved successfully", agentName);
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());

		Long moveDuration = Math.abs(timestamp.getTime() - moveTime.getTime());
//		try {
//			FileOutputStream fos = new FileOutputStream("times.txt", true);
//			fos.write((moveDuration.toString() + "\n").getBytes());
//			fos.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		log("agent [] finished moving at []", agentName, timestamp);

		log("agent [] finished moving in [] milliseconds", agentName, moveDuration);
//		start();
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_START);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
		return postAgentEvent(prepareMoveEvent);
    }

	/**
	 * Loads shards after moving.
	 */
	public void loadShards() {
		localLog = new UnitComponent().setLoggerType(PlatformUtils.platformLogType()).setLogLevel(Level.ALL);
		loader = new CompositeAgentLoader();
		loader.configure(new MultiTreeMap(), localLog, PlatformUtils.getClassFactory());
		//System.out.println("shards before load " + shards);
		serializedShards.forEach((designation, serializedShard) ->
		addShard(deserializeShard(serializedShard))
		);
		nonSerializedShardDesignations.forEach((designation, configuration) -> {
			loader.preloadShard(designation.toString(), configuration, null, "PRE_LOADING_NON-SERIALIZED_SHARDS: ");
			AgentShard shard = loader.loadShard(designation.toString(), configuration,
					"LOADING_NON-SERIALIZED_SHARDS: ", agentName);
			addShard(shard);
		});
		shards.values().forEach(shard -> {
			// ShardContainer container = new CompositeAgentShardContainer(this);
			shard.addContext(asContext);
		});

		log("agent [] has shards [] after deserialization", agentName, shards);
//		System.out.println("### DESERIALIZED SHARDS: " + shards);
	}

	/**
	 * @param serializedShard - serialized shard in String form.
	 * Deserializes a shard.
	 */
	private AgentShard deserializeShard(String serializedShard) {
		AgentShard agentShard = null;
		ByteArrayInputStream fis;
		ObjectInputStream in;
		try {
			fis = new ByteArrayInputStream(Base64.getDecoder().decode(serializedShard));
			in = new ObjectInputStream(fis);
			agentShard = (AgentShard) in.readObject();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return agentShard;
	}

	/**
	 * @param agentData - serialized agent in String form.
	 * Deserializes an agent.
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
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return agent;
	}

	/**
	 * Serializes the current agent after:
	 * - sorting the shards between serializable and non-serializable
	 * - emptying shards and shardOrder
	 */
    public String serialize() {
		for(AgentShardDesignation designation : shards.keySet()) {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(shards.get(designation));

				log("serializable shard: []", shards.get(designation));
//				System.out.println("### SERIALIZABLE SHARD: " + shards.get(designation));
                serializedShards.put(designation, Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
			} catch (NotSerializableException e) {
//				e.printStackTrace();

				log("non-serializable shard: []", shards.get(designation));
//				System.out.println("### NON-SERIALIZABLE SHARD: " + shards.get(designation));
				MultiTreeMap configuration = shards.get(designation) instanceof NonSerializableShard
						? ((NonSerializableShard) shards.get(designation)).getShardConfiguration()
						: null;
				nonSerializedShardDesignations.put(designation, configuration);
            } catch (IOException e) {
                //TODO
            }
        }

        shards = new HashMap<>();
		shardOrder = new ArrayList<>();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

	/**
	 * Overrides the implementation in {@link CompositeAgent}. When an event with a
	 * {@link MobileCompositeAgent#MOVE_TRANSIENT_EVENT_PARAMETER} is received, the agent is packaged
	 * in a JSON with the destination and sent to the node proxy.
	 */
	@Override
	protected AgentEvent eventProcessingCycle() {
		AgentEvent exitEvent = super.eventProcessingCycle();

		if(exitEvent != null && MOVE_TRANSIENT_EVENT_PARAMETER.equals(exitEvent.get(TRANSIENT_EVENT_PARAMETER))) {
			Node.NodeProxy nodeProxy = getNodeProxyContext();

			List<EntityProxy<? extends Entity<?>>> contexts = new ArrayList<>(agentContext);
			contexts.forEach(this::removeGeneralContext);

			JsonObject root = new JsonObject();
			root.addProperty(OperationUtils.NAME, OperationUtils.ControlOperation.RECEIVE_AGENT.toString().toLowerCase());

			String destination = exitEvent.getValue(TARGET);
			root.addProperty(OperationUtils.PARAMETERS, destination);

			String agentData = serialize();
			root.addProperty("agentData", agentData);

			String json = root.toString();

			if(nodeProxy != null) {
				nodeProxy.moveAgent(destination, agentName, json);
			}
		}
		return exitEvent;
	}
}
