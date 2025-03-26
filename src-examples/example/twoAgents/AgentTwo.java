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
package example.twoAgents;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;

/**
 * Simple agent for testing.
 * 
 * @author Andrei Olaru
 */
public class AgentTwo extends BaseAgent {
	
	/**
	 * The messaging shard.
	 */
	MessagingShard msgShard;
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		// the pylon proxy should be added by now
		msgShard = (MessagingShard) AgentShardCore.instantiateRecommendedShard(StandardAgentShard.MESSAGING,
				(PylonProxy) getContext(), null, new BaseAgentProxy() {
					@Override
					public boolean postAgentEvent(AgentEvent event) {
						return processEvent(event);
					}
				});
		msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
		li("Agent started");
		return true;
	}
	
	/**
	 * Processes the event and sends a reply.
	 * 
	 * @param e
	 *            - event received.
	 * @return <code>true</code> if all good.
	 */
	protected boolean processEvent(AgentEvent e) {
		if(!AgentEventType.AGENT_WAVE.equals(e.getType()))
			return false;
		li("received:", e);
		String replyContent = ((AgentWave) e).getContent() + " back";
		AgentWave m = ((AgentWave) e).createReply(replyContent);
		li("sending reply:", m);
		boolean result = msgShard != null ? msgShard.sendMessage(m) : false;
		stop();
		return result;
	}
}
