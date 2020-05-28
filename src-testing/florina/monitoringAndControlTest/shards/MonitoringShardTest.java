package florina.monitoringAndControlTest.shards;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class MonitoringShardTest extends AgentShardGeneral {

    public MonitoringShardTest() {
        super(AgentShardDesignation.customShard(FUNCTIONALITY));
    }

    /**
     * The UID.
     */
    private static final long	serialVersionUID	= 521488201837501593L;

    /**
     * Cache for the name of this agent.
     */
    String							thisAgent					= null;

    /**
     * Endpoint element of this shard.
     */
    public static final String  SHARD_ENDPOINT      = "monitoring";

    public static final String	FUNCTIONALITY	    = "MONITORING";

    {
        setUnitName("monitoring-shard").setLoggerType(PlatformUtils.platformLogType());
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        return super.configure(configuration);
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        switch (event.getType())
        {
            case AGENT_WAVE:
                if(!(((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT))
                    break;
                parseAgentWaveEvent(((AgentWave)event).getContent());
                break;
            case AGENT_START:
                li("Shard []/[] started.", thisAgent, SHARD_ENDPOINT);
                sendStatusUpdate(event.getType().toString());
                break;
            case AGENT_STOP:
                li("Shard []/[] stopped.", thisAgent, SHARD_ENDPOINT);
                sendStatusUpdate(event.getType().toString());
                break;
            case SIMULATION_START:
                li("Shard []/[] started simulation.", thisAgent, SHARD_ENDPOINT);
                sendStatusUpdate(event.getType().toString());
                break;
            case SIMULATION_PAUSE:
                li("Shard []/[] paused simulation.", thisAgent, SHARD_ENDPOINT);
                sendStatusUpdate(event.getType().toString());
                break;
            default:
                break;
        }
    }

    private void parseAgentWaveEvent(String content) {
        JSONObject jsonObject = (JSONObject) JSONValue.parse(content);
        if(jsonObject == null)
            le("null jsonObject received at []/[]", thisAgent, SHARD_ENDPOINT);
    }

    @Override
    protected void parentChangeNotifier(ShardContainer oldParent) {
        super.parentChangeNotifier(oldParent);
        if(getAgent() != null)
            thisAgent = getAgent().getEntityName();
    }

    private void sendStatusUpdate(String status) {
        JSONObject content = new JSONObject();
        content.put("operation", "state-update");
        content.put("params", getAgent().getEntityName());
        content.put("value", status);
        sendMessage(content.toString(), SHARD_ENDPOINT, DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
    }

    @Override
    protected MultiTreeMap getShardData() {
        return super.getShardData();
    }
}
