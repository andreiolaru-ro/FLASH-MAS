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
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import test.compositePingPong.Boot;

/**
 * An {@link AgentShard} implementation that sends messages to other agents.
 * <p>
 * This is a rather older implementation, that starts pinging immediately after agent start.
 * <p>
 * Since it is a testing mechanism, it will instruct the agent to terminate immediately after the last of the specified
 * number of pings is sent. Termination can be suppressed by sending the {@value #KEEP_PARAMETER_NAME} parameter.
 * <p>
 * Pings are sent indefinitely if n < 0.
 * 
 * @author Andrei Olaru
 */
public class PingTestShard extends AgentShardGeneral {
	/**
	 * The instance sends a message to the "other agent".
	 * 
	 * @author Andrei Olaru
	 */
	class Pinger extends TimerTask {
		@Override
		public void run() {
			tick++;
			System.out.println("Sending the message....");
			if(n >= 0 && tick >= n) {
				sendMessage(PING_PRE_LAST + tick);
				endActivity();
			}
			else
				sendMessage(PING_PRE + tick);
		}
	}
	
	/**
	 * The UID.
	 */
	private static final long		serialVersionUID			= 5214882018809437402L;
	/**
	 * The name of the parameter that contains the id(s) of the other agent(s).
	 */
	protected static final String	OTHER_AGENT_PARAMETER_NAME	= "otherAgent";
	/**
	 * The name of the parameter that contains the period between pings.
	 */
	protected static final String	TIME_PARAMETER_NAME			= "every";
	/**
	 * The name of the parameter that contains the total number of pings.
	 */
	protected static final String	N_PARAMETER_NAME			= "n";
	/**
	 * The name of the parameter that indicates agent should not be terminated after the last ping.
	 */
	protected static final String	KEEP_PARAMETER_NAME			= "keep";
	/**
	 * The prefix for every message except the last.
	 */
	protected static final String	PING_PRE					= "ping-no ";
	/**
	 * The prefix for the last message.
	 */
	protected static final String	PING_PRE_LAST				= "ping-last ";
	/**
	 * Endpoint element for this shard.
	 */
	protected static final String	SHARD_ENDPOINT				= "ping";
	/**
	 * Initial delay before the first ping message.
	 */
	protected static final long		PING_INITIAL_DELAY			= 0;
	/**
	 * Time between ping messages.
	 */
	protected static final int		DEFAULT_PING_PERIOD			= 2000;
	/**
	 * Number of pings.
	 */
	protected static final int		DEFAULT_NUMBER				= 5;
	
	/**
	 * Timer for pinging.
	 */
	transient Timer	pingTimer	= null;
	/**
	 * Cache for the name of this agent.
	 */
	String			thisAgent	= null;
	/**
	 * Cache for the name of the other agent.
	 */
	List<String>	otherAgents	= null;
	/**
	 * The index of the message sent.
	 */
	int				tick		= 0;
	/**
	 * Period between pings.
	 */
	int				period;
	/**
	 * Number of pings to send in total.
	 */
	int				n;
	/**
	 * If the agent should be kept alive after the last ping is sent.
	 */
	boolean			keep		= false;
	
	/**
	 * No-argument constructor
	 */
	public PingTestShard() {
		super(AgentShardDesignation.customShard(Boot.FUNCTIONALITY));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		otherAgents = configuration.getValues(OTHER_AGENT_PARAMETER_NAME);
		period = configuration.containsKey(TIME_PARAMETER_NAME)
				? Integer.parseInt(configuration.getFirstValue(TIME_PARAMETER_NAME))
				: DEFAULT_PING_PERIOD;
		n = configuration.containsKey(N_PARAMETER_NAME)
				? Integer.parseInt(configuration.getFirstValue(N_PARAMETER_NAME))
				: DEFAULT_NUMBER;
		if(configuration.containsKey(KEEP_PARAMETER_NAME))
			keep = true;
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			pingTimer = new Timer();
			pingTimer.schedule(new Pinger(), PING_INITIAL_DELAY, DEFAULT_PING_PERIOD);
			break;
		case AGENT_STOP:
			pingTimer.cancel();
			break;
		default:
			//
		}
	}
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		if(getAgent() != null)
			thisAgent = getAgent().getEntityName();
	}
	
	/**
	 * Relays.
	 * 
	 * @param content
	 * @return a success indication.
	 */
	protected boolean sendMessage(String content) {
		boolean res = true;
		for(String otherAgent : otherAgents)
			res &= sendMessage(content, SHARD_ENDPOINT, otherAgent, PingBackTestShard.SHARD_ENDPOINT);
		return res;
	}
	
	/**
	 * Wrap up the activity of this shard and instructs the agent to stop.
	 */
	protected void endActivity() {
		pingTimer.cancel();
		if(!keep)
			getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
	}
}
