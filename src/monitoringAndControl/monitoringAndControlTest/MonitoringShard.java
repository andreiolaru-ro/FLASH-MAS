package monitoringAndControl.monitoringAndControlTest;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;

public class MonitoringShard extends AgentShardGeneral
{
    /**
     * The UID.
     */
    private static final long		serialVersionUID			= 5214882018809437402L;

    /**
     * Endpoint element for this shard.
     */
    protected static final String	SHARD_ENDPOINT				= "monitoring";

    /**
     * Cache for the name of this agent.
     */
    String							thisAgent					= null;


    public static final String	FUNCTIONALITY	= "MONITORING";



    /**
     * Default constructor
     */
    public MonitoringShard()
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
            case AGENT_START:
                System.out.println("## MONITORING SHARD STARTED");
                break;
            case AGENT_WAVE:
                parseAgentWave(((AgentWave)event).getContent());
                break;
            case SIMULATION_START:
                System.out.println("## MONITORING SHARD SIMULATION STARTED");
                break;
            default:
                break;
        }
    }

    protected void parseAgentWave(String command)
    {
        switch (command)
        {
            case "stop":
                getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
                break;
            case "start":
                getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
                break;
            case "simulation":
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

