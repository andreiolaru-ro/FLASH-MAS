package maria;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils;

public class MobileCompositeAgent extends CompositeAgent implements Serializable {

	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -308343471716425142L;
	
	public static final String MOVE_TRANSIENT_EVENT_PARAMETER = "MOVE";

    public static final String TARGET = "TARGET";

    public Map<AgentShardDesignation, String> serializedShards = new HashMap<>();

    public List<AgentShardDesignation> nonserializedShardDesignations = new ArrayList<>();

	public MobileCompositeAgent() {
		System.out.println("Constructing after move, my name is " + agentName);
	}
	
	public MobileCompositeAgent(MultiTreeMap configuration) {
		super(configuration);
		System.out.println("Constructing after move, my name is " + agentName);
	}
	
    public void moveTo(String destination) {
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
        prepareMoveEvent.add(TARGET, destination);
        postAgentEvent(prepareMoveEvent);
    }

    public void startAfterMove() {
        // adaugare transient event parameter
		System.out.println("Starting after move, my name is " + agentName);
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_START);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
        postAgentEvent(prepareMoveEvent);
    }

    public String serialize() {
        //TODO: serialize shards one by one
		System.out.println("Serializing, my name is " + agentName);
		
		for(var designation : shards.keySet()) {
            // try - catch exception
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(shards.get(designation));
                objectOutputStream.close();

                serializedShards.put(designation, Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
            } catch (NotSerializableException e) {
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
			
			JSONObject root = new JSONObject();
			root.put(OperationUtils.NAME, OperationUtils.ControlOperation.RECEIVE_AGENT.toString().toLowerCase());
			
			String destination = exitEvent.getValue(TARGET);
			root.put(OperationUtils.PARAMETERS, destination);
			
			String agentData = serialize();
			root.put("agentData", agentData);
			
			String json = root.toJSONString();
			
			if(nodeProxy != null) {
				nodeProxy.moveAgent(destination, json);
			}
		}
		return exitEvent;
	}
}
