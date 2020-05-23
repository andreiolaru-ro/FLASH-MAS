package florina.monitoringAndControlTest;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.Unit;

public class ControlShardTest extends AgentShardGeneral
{
    /**
     * The UID.
     */
    private static final long		serialVersionUID			= 5214882018809437402L;

    /**
     * Endpoint element for this shard.
     */
    protected static final String	SHARD_ENDPOINT				= "control";

    /**
     * Cache for the name of this agent.
     */
    String							thisAgent					= null;


    public static final String	FUNCTIONALITY	                = "CONTROL";

    {
        setUnitName("control-shard").setLoggerType(PlatformUtils.platformLogType());
    }

    /**
     * Default constructor
     */
    public ControlShardTest()
    {
        super(AgentShardDesignation.customShard(FUNCTIONALITY));
    }

    @Override
    public boolean configure(MultiTreeMap configuration)
    {
        if(!super.configure(configuration))
            return false;
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event)
    {
        super.signalAgentEvent(event);
        switch(event.getType())
        {
            case AGENT_WAVE:
                parseAgentWave(event);
                break;
            case AGENT_START:
                li("Shard of agent [] started.", thisAgent);
                break;
            case SIMULATION_START:
                li("Shard of agent [] started simulation.", thisAgent);
                break;
            case AGENT_STOP:
                li("Shard of agent [] stopped.", thisAgent);
                break;
            default:
                break;
        }
    }

    protected void parseAgentWave(AgentEvent event)
    {
        if(!((AgentWave)event).getFirstDestinationElement().equals(SHARD_ENDPOINT)) return;

        switch (((AgentWave)event).getContent())
        {
            case "stop":
                getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
                break;
            case "start_simulation":
                getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.SIMULATION_START));
            default:
                break;
        }
    }

    @Override
    protected void parentChangeNotifier(ShardContainer oldParent)
    {
        super.parentChangeNotifier(oldParent);
        if(getAgent() != null)
            thisAgent = getAgent().getEntityName();
    }


    protected boolean sendMessage(String content)
    {
        //TODO: messages are sent via messaging shard
        //return sendMessage(content, SHARD_ENDPOINT, otherAgent, PingBackTestComponent.SHARD_ENDPOINT);
        return true;
    }

    @Override
    protected MultiTreeMap getShardData()
    {
        return super.getShardData();
    }

}

