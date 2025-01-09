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

import java.util.Timer;
import java.util.TimerTask;

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
public class AgentOne extends BaseAgent {
	
	/**
	 * Content to send to the other agent.
	 */
	protected static final String	MESSAGE_CONTENT	= "Hello";
	/**
	 * The other agent.
	 */
	protected static final String	OTHER_AGENT		= "AgentTwo";
	/**
	 * The messaging shard.
	 */
	MessagingShard					msgShard;
	
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
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendPing();
				timer.cancel();
			}
		}, 1000);
		return true;
	}
	
	/**
	 * Sends initial message.
	 */
	protected void sendPing() {
		AgentWave m = new AgentWave(MESSAGE_CONTENT, OTHER_AGENT);
		li("sending message:", m);
		msgShard.sendMessage(m);
	}
	
	/**
	 * Processes the event.
	 * 
	 * @param e
	 *            - event received.
	 * @return <code>true</code> if all good.
	 */
	protected boolean processEvent(AgentEvent e) {
		li("Event received:", e);
		stop();
		return true;
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		li("Agent stopping");
		return true;
	}
}
