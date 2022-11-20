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
package testing;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import test.compositePingPong.Boot;

/**
 * An {@link AgentShard} implementation that initially sends a message to another agent, it this agent is designated as
 * initiator.
 * <p>
 * Otherwise, it waits for a ping message, that it then sends back.
 * 
 * @author Andrei Olaru
 */
public class PingBackTestShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long	serialVersionUID	= 5214882018809437402L;
	/**
	 * Endpoint element for this shard.
	 */
	public static final String	SHARD_ENDPOINT		= "pong";
	
	/**
	 * No-argument constructor
	 */
	public PingBackTestShard() {
		super(AgentShardDesignation.customShard(Boot.FUNCTIONALITY));
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_WAVE:
			if(!(((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT))
				break;
			String replyContent = ((AgentWave) event).getContent() + " reply";
			sendMessage(replyContent, SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
			break;
		default:
			break;
		}
	}
}
