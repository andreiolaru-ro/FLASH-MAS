/*******************************************************************************
 * Copyright (C) 2015 Andrei Olaru, Marius-Tudor Benea, Nguyen Thi Thuy Nga, Amal El Fallah Seghrouchni, Cedric Herpson.
 * 
 * This file is part of tATAmI-PC.
 * 
 * tATAmI-PC is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * tATAmI-PC is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with tATAmI-PC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package examples.compositeAgent;

import deploymentTest.DeploymentTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

/**
 * An {@link AgentShard} implementation that initially sends a message to another agent, it this agent is designated as
 * initiator.
 * <p>
 * Otherwise, it waits for a ping message, that it then sends back.
 * 
 * @author Andrei Olaru
 */
public class PingBackTestComponent extends AgentShardGeneral
{
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = 5214882018809437402L;
	
	/**
	 * Default constructor
	 */
	public PingBackTestComponent()
	{
		super(AgentShardDesignation.customShard(DeploymentTest.FUNCTIONALITY));
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		super.signalAgentEvent(event);
		switch(event.getType())
		{
		case AGENT_WAVE:
			String replyContent = ((AgentWave) event).getContent() + " reply";
			getMessagingShard().sendMessage(getMessagingShard().getAgentAddress(),
					((AgentWave) event).getCompleteSource(), replyContent);
			break;
		default:
			break;
		}
	}
}
