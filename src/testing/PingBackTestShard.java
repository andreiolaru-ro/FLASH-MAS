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
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * An {@link AgentShard} implementation that waits for a ping message, then sends back a reply.
 * <p>
 * If a message beginning with "ping-last" is received, it will instruct the agent to terminate.
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
	 * Suffix to add when replying to messages.
	 */
	public static final String	REPLY_SUFFIX		= " reply";
	/**
	 * If the agent should be kept alive after the last ping is received.
	 */
	boolean						keep				= false;
	
	/**
	 * Tested functionality.
	 */
	public static final String FUNCTIONALITY = "MOBILITY";
	
	/**
	 * No-argument constructor
	 */
	public PingBackTestShard() {
		super(AgentShardDesignation.customShard(FUNCTIONALITY));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.containsKey(PingTestShard.KEEP_PARAMETER_NAME))
			keep = true;
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_WAVE:
			// if(!(((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT))
			// break;
			String replyContent = ((AgentWave) event).getContent() + REPLY_SUFFIX;
			sendMessage(((AgentWave) event).createReply(replyContent));
			if(replyContent.startsWith(PingTestShard.PING_PRE_LAST) && !keep)
				getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
			break;
		default:
			break;
		}
	}
}
