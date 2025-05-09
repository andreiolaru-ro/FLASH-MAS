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
package florina.monitoringAndControlTest.shards;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;

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
		setUnitName("control-shard");
		setLoggerType(PlatformUtils.platformLogType());
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
                if(!(((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT))
                    break;
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
}

