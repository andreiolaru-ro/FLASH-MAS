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

import net.xqhs.flash.core.agent.AgentFeature;
import net.xqhs.flash.core.agent.composite.AgentEvent;
import net.xqhs.flash.core.agent.composite.AgentEvent.AgentEventHandler;
import net.xqhs.flash.core.agent.composite.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation;
import net.xqhs.flash.core.agent.composite.CompositeAgent;
import net.xqhs.flash.core.feature.CompositeAgentFeatureEx;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

/**
 * An {@link AgentFeature} implementation that monitors all agent events.
 * 
 * @author Andrei Olaru
 */
public class MonitoringTestFeature extends CompositeAgentFeatureEx
{
	/**
	 * The UID.
	 */
	private static final long	serialVersionUID	= 5214882018809437402L;
	/**
	 * The log.
	 */
	UnitComponent				locallog			= null;
	
	/**
	 * Default constructor
	 */
	public MonitoringTestFeature()
	{
		super(AgentFeatureDesignation.customFeature("TESTING"));
	}
	
	@Override
	protected String getAgentName()
	{
		return super.getAgentName();
	}
	
	@Override
	protected Logger getAgentLog()
	{
		return super.getAgentLog();
	}
	
	@Override
	protected void featureInitializer()
	{
		super.featureInitializer();
		
		AgentEventHandler allEventHandler = new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				String eventMessage = "agent [" + getAgentName() + "] event: [" + event.toString() + "]";
				locallog.li(eventMessage);
				if(getAgentLog() != null)
					getAgentLog().info(eventMessage);
				if(event.getType() == AgentEventType.AGENT_STOP)
					locallog.doExit();
			}
		};
		for(AgentEventType eventType : AgentEventType.values())
			registerHandler(eventType, allEventHandler);
	}
	
	@Override
	protected void parentChangeNotifier(CompositeAgent oldParent)
	{
		super.parentChangeNotifier(oldParent);
		
		if(getAgent() != null)
		{
			locallog = (UnitComponent) new UnitComponent().setUnitName("monitoring-" + getAgentName()).setLogLevel(
					Level.ALL);
			locallog.lf("testing started.");
		}
		else if(locallog != null)
		{
			locallog.doExit();
			locallog = null;
		}
	}
}
