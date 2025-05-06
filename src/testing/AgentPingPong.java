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
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;

/**
 * An extension of AgentPingPongPlain using MessagingShard instead of WaveMessagingPylonProxy.
 */
public class AgentPingPong extends AgentPingPongPlain {
	/**
	 * The serial UID.
	 */
	private static final long	 	serialVersionUID	 		= 1L;
	/**
	 * The messaging shard.
	 */
	MessagingShard	 msgShard	 = null;

	@Override
	public boolean start() {
		if(!super.start())
			return false;
		if(msgShard == null)
			throw new IllegalStateException("No messaging shard present");
		msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
		return true;
	}
	
	/**
	 * @param event
	 *            - the event received.
	 * @return <code>true</code> if the message was posted successfully.
	 */
	protected boolean processEvent(AgentEvent event) {
		li("received: " + event.toString());
		if(AgentEventType.AGENT_WAVE.equals(event.getType()) && otherAgents == null) {
			// only reply if this is not the ping agent
			String replyContent = ((AgentWave) event).getContent() + " reply";
			li("sending reply ", ((AgentWave) event).createReply(replyContent));
			return msgShard.sendMessage(((AgentWave) event).createReply(replyContent));
		}
		return false;
	}
	
	@Override
	public boolean stop() {
		if (!super.stop()) return false;
		return true;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		if(!super.addContext(context))
			return false;
		msgShard = (MessagingShard) AgentShardCore.instantiateRecommendedShard(StandardAgentShard.MESSAGING,
				(PylonProxy) context, null, new BaseAgentProxy() {
					@Override
					public boolean postAgentEvent(AgentEvent event) {
						return processEvent(event);
					}
				});
		lf("Context added: ", context.getEntityName());
		return msgShard.addGeneralContext(context);
	}
	
	/**
	 * Pings the other agents.
	 */
	protected void sendPing() {
		if (pingLimit >= 0 && tick >= pingLimit) {
			li("Ping limit reached, stopping agent.");
			stop();
			return;
		}

		tick++;
		for(String otherAgent : otherAgents) {
			AgentWave wave = new AgentWave("ping-no " + tick, otherAgent, "pong").addSourceElementFirst("ping");
			lf("Sending the message [] to ", wave, otherAgent);
			if(!msgShard.sendMessage(wave))
				le("Message sending failed");
		}
	}
}
