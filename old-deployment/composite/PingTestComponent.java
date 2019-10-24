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
package examples.composite;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventHandler;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.logging.Logger;

/**
 * An {@link AgentShard} implementation that sends messages to other agents.
 * <p>
 * This is a rather older implementation, that starts pinging immediately after agent start.
 * 
 * @author Andrei Olaru
 */
public class PingTestComponent extends AgentShardCore
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
			
			sendMessage("ping no " + tick, thisAgent, otherAgent);
		}
		
	}
	
	/**
	 * The UID.
	 */
	private static final long		serialVersionUID			= 5214882018809437402L;
	/**
	 * The name of the component parameter that contains the id of the other agent.
	 */
	protected static final String	OTHER_AGENT_PARAMETER_NAME	= "other agent";
	/**
	 * Initial delay before the first ping message.
	 */
	protected static final long		PING_INITIAL_DELAY			= 0;
	/**
	 * Time between ping messages.
	 */
	protected static final long		PING_PERIOD					= 1000;
	
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
		super(AgentComponentName.TESTING_COMPONENT);
	}
	
	@Override
	protected boolean preload(ComponentCreationData parameters, XMLNode scenarioNode, List<String> agentPackages,
			Logger log)
	{
		if(!super.preload(parameters, scenarioNode, agentPackages, log))
			return false;
		otherAgent = getComponentData().get(OTHER_AGENT_PARAMETER_NAME);
		return true;
	}
	
	/**
	 * @return the log provided by the visualizable component.
	 */
	@Override
	public Logger getAgentLog()
	{
		return super.getAgentLog();
	}
	
	@Override
	protected void componentInitializer()
	{
		super.componentInitializer();
		
		AgentEventHandler allEventHandler = new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				getAgentLog().li("agent [] event: []", thisAgent, event.getType());
				
				if(event.getType() == AgentEventType.AGENT_START)
					atAgentStart(event);
				if(event.getType() == AgentEventType.SIMULATION_START)
					atSimulationStart(event);
			}
		};
		for(AgentEventType eventType : AgentEventType.values())
			registerHandler(eventType, allEventHandler);
	}
	
	@Override
	protected void atAgentStart(AgentEvent event)
	{
		super.atAgentStart(event);
		
		registerMessageReceiver(new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent messageEvent)
			{
				getAgentLog().li("message received: ", messageEvent);
			}
		}, "");
		
		pingTimer = new Timer();
	}
	
	@Override
	protected void atSimulationStart(AgentEvent event)
	{
		super.atSimulationStart(event);
		pingTimer.schedule(new Pinger(), PING_INITIAL_DELAY, PING_PERIOD);
	}
	
	@Override
	protected void parentChangeNotifier(CompositeAgent oldParent)
	{
		super.parentChangeNotifier(oldParent);
		
		if(getParent() != null)
		{
			thisAgent = getAgentName();
		}
	}
	
	/**
	 * Relay.
	 */
	@Override
	protected boolean sendMessage(String content, String sourceEndpoint, String targetAgent,
			String... targetPathElements)
	{
		return super.sendMessage(content, sourceEndpoint, targetAgent, targetPathElements);
	}
}
