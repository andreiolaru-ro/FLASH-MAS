package maria;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;

public class MobileCompositeAgent extends CompositeAgent {

    public static final String MOVE_TRANSIENT_EVENT_PARAMETER = "MOVE";

    public static final String TARGET = "TARGET";

    public Map<AgentShardDesignation, String> serializedShards = new HashMap<>();

    public List<AgentShardDesignation> nonserializedShardDesignations = new ArrayList<>();


    public MobileCompositeAgent(MultiTreeMap configuration) {
        agentName = configuration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
//        setUnitName(agentName);
    }

    public void moveTo(String destination) {
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
        prepareMoveEvent.add(TARGET, destination);
        postAgentEvent(prepareMoveEvent);
    }

    public void startAfterMove() {
        // adaugare transient event parameter
        AgentEvent prepareMoveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_START);
        prepareMoveEvent.add(TRANSIENT_EVENT_PARAMETER, MOVE_TRANSIENT_EVENT_PARAMETER);
        postAgentEvent(prepareMoveEvent);
    }

    public String serialize() {
        //TODO: serialize shards one by one
        for (var designation : shards.keySet()) {
            // try - catch exception
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream;

                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
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
    protected void eventProcessingCycle()
    {
        boolean threadExit = false;
        while(!threadExit)
        {
            if(eventQueue == null)
            {
                log("No event queue present");
                return;
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
                        for(AgentShard shard : shardOrder)
                            shard.signalAgentEvent(event);
                        break;
                    case DESTRUCTIVE:
                        for(ListIterator<AgentShard> it = shardOrder.listIterator(shardOrder.size()); it.hasPrevious();)
                            it.previous().signalAgentEvent(event);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unsupported sequence type: " + event.getType().getSequenceType().toString());
                }

                threadExit = FSMEventOut(event.getType(), event.isSet(TRANSIENT_EVENT_PARAMETER));

                if (MOVE_TRANSIENT_EVENT_PARAMETER.equals(event.get(TRANSIENT_EVENT_PARAMETER))) {
                    Node.NodeProxy nodeProxy = getNodeProxyContext();

                    List<EntityProxy<? extends Entity<?>>> contexts = new ArrayList<>(agentContext);
                    contexts.forEach(this::removeGeneralContext);

                    JSONObject root = new JSONObject();
                    root.put(OperationUtils.NAME, OperationUtils.ControlOperation.RECEIVE_AGENT.toString().toLowerCase());

                    String destination = event.getValue(TARGET);
                    root.put(OperationUtils.PARAMETERS, destination);

                    String agentData = serialize();
                    root.put("agentData", agentData);

                    String json = root.toJSONString();

                    if (nodeProxy != null) {
                        nodeProxy.moveAgent(destination, json);
                    }
                }
            }
        }
    }
}
