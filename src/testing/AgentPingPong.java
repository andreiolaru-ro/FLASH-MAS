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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * The implementation of the agents.
 */
public class AgentPingPong extends BaseAgent {
	/**
	 * The serial UID.
	 */
	private static final long		serialVersionUID			= 2098318382514974043L;
	/**
	 * The name of the component parameter that contains the id of the other agent.
	 */
	protected static final String	OTHER_AGENT_PARAMETER_NAME	= "sendTo";
	/**
	 * Endpoint element for this shard.
	 */
	protected static final String	SHARD_ENDPOINT				= "ping";
	/**
	 * Initial delay before the first ping message.
	 */
	
	// For WebSocket we need to wait until all agents are started and registered to the server.
	protected static final long		PING_INITIAL_DELAY			= 2000;
	/**
	 * Time between ping messages.
	 */
	protected static final long		PING_PERIOD					= 2000;
	
	/**
	 * Timer for pinging.
	 */
	Timer			pingTimer	= null;
	/**
	 * Cache for the name of the other agent.
	 */
	List<String>	otherAgents	= null;
	/**
	 * The messaging shard.
	 */
	MessagingShard	msgShard	= null;
	/**
	 * The name of this agent.
	 */
	String			agentName	= null;
	/**
	 * The index of the message sent.
	 */
	int				tick		= 0;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.isSet(OTHER_AGENT_PARAMETER_NAME))
			otherAgents = configuration.getValues(OTHER_AGENT_PARAMETER_NAME);
		return true;
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		if(msgShard == null)
			throw new IllegalStateException("No messaging shard present");
		msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
		if(otherAgents != null) {
			// agent is Ping agent.
			pingTimer = new Timer();
			pingTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					sendPing();
				}
			}, PING_INITIAL_DELAY, PING_PERIOD);
		}
		li("Agent started");
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
		if(!super.stop())
			return false;
		pingTimer.cancel();
		li("Agent stopped");
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
		tick++;
		for(String otherAgent : otherAgents) {
			AgentWave wave = new AgentWave("ping-no " + tick, otherAgent, "pong").addSourceElementFirst("ping");
			lf("Sending the message [] to ", wave, otherAgent);
			if(!msgShard.sendMessage(wave))
				le("Message sending failed");
		}
	}
}
