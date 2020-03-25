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
package examples.compositePingPong;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * An {@link AgentShard} implementation that sends messages to other agents.
 * <p>
 * This is a rather older implementation, that starts pinging immediately after agent start.
 * 
 * @author Andrei Olaru
 */
public class PingTestComponent extends AgentShardGeneral
{
	/**
	 * The instance sends a message to the "other agent".
	 * 
	 * @author Andrei Olaru
	 */
	class Pinger extends TimerTask
	{
		/**
		 * The index of the message sent.
		 */
		int	tick	= 0;
		
		@Override
		public void run()
		{
			tick++;
			System.out.println("Sending the message....");
			sendMessage("ping-no " + tick);
		}
		
	}
	
	/**
	 * The UID.
	 */
	private static final long		serialVersionUID			= 5214882018809437402L;
	/**
	 * The name of the component parameter that contains the id of the other agent.
	 */
	protected static final String	OTHER_AGENT_PARAMETER_NAME	= "otherAgent";
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
	protected static final long		PING_PERIOD					= 2000;
	
	/**
	 * Timer for pinging.
	 */
	Timer							pingTimer					= null;
	/**
	 * Cache for the name of this agent.
	 */
	String							thisAgent					= null;
	/**
	 * Cache for the name of the other agent.
	 */
	String							otherAgent					= null;
	
	/**
	 * Default constructor
	 */
	public PingTestComponent()
	{
		super(AgentShardDesignation.customShard(Boot.FUNCTIONALITY));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration)
	{
		if(!super.configure(configuration))
			return false;
		otherAgent = configuration.getFirstValue(OTHER_AGENT_PARAMETER_NAME);
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		super.signalAgentEvent(event);
		switch(event.getType())
		{
		case AGENT_START:
			pingTimer = new Timer();
			pingTimer.schedule(new Pinger(), PING_INITIAL_DELAY, PING_PERIOD);
			break;
		case SIMULATION_START:
			break;
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
	
	/**
	 * Relays.
	 * 
	 * @param content
	 * @return a success indication.
	 */
	protected boolean sendMessage(String content)
	{
		return sendMessage(content, SHARD_ENDPOINT, otherAgent, PingBackTestComponent.SHARD_ENDPOINT);
	}
	
	@Override
	protected MultiTreeMap getShardData()
	{
		return super.getShardData();
	}
}
