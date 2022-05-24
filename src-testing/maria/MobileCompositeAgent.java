package maria;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

public class MobileCompositeAgent extends CompositeAgent implements Serializable {

	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -308343471716425142L;

	public static final String MOVE_TRANSIENT_EVENT_PARAMETER = "MOVE";

    public static final String TARGET = "TARGET";

	public boolean deserialized = false;

    public Map<AgentShardDesignation, String> serializedShards = new HashMap<>();

    public List<AgentShardDesignation> nonserializedShardDesignations = new ArrayList<>();

	private transient CompositeAgentLoader loader = new CompositeAgentLoader();

	public MobileCompositeAgent() {
	}

	public MobileCompositeAgent(MultiTreeMap configuration) {
		super(configuration);
	}
	
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

    public void moveTo(String destination) {
		System.out.println("### SHARDS BEFORE SERIALIZATION: " + shards);
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
        prepareMoveEvent.add(TARGET, destination);
        postAgentEvent(prepareMoveEvent);
    }

    public void startAfterMove() {
        // adaugare transient event parameter
		System.out.println("Starting after move, my name is " + agentName);
//		start();
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_START);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
        postAgentEvent(prepareMoveEvent);
    }

	public void loadShards() {
		loader = new CompositeAgentLoader();
		System.out.println("shards before load " + shards);
		serializedShards.forEach((designation, serializedShard) ->
			shards.put(designation, deserializeShard(serializedShard))
		);
		System.out.println("### DESERIALIZED SHARDS: " + shards);
		nonserializedShardDesignations.forEach(designation -> {
			loader.preloadShard(designation.toString(), null, null, "PRE_LOADING_NON-SERIALIZED_SHARDS: ");
			loader.loadShard(designation.toString(), null, "LOADING_NON-SERIALIZED_SHARDS: ", agentName);
		});
	}

	public void addAsParentAgent() {
		shards.values().forEach(shard -> {
			ShardContainer container = new CompositeAgentShardContainer(this);
			shard.addContext(container);
		});
	}
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

    public String serialize() {
		for(AgentShardDesignation designation : shards.keySet()) {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(shards.get(designation));
				System.out.println("SERIALIZABLE SHARD: " + shards.get(designation));
                serializedShards.put(designation, Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
			} catch (NotSerializableException e) {
				e.printStackTrace();
				System.out.println("NONSERIALIZABLE SHARD: " + shards.get(designation));
				nonserializedShardDesignations.add(designation);
            } catch (IOException e) {
                //TODO
            }
        }

        shards = new HashMap<>();

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
