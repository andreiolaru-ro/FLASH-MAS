/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.agent;

import net.xqhs.flash.core.util.MultiValueMap;

/**
 * The class models an agent event, characterized by its type and, optionally, a set of parameters that have names.
 * <p>
 * The class extends {@link MultiValueMap}, so the same methods can be used to manage parameters.
 * <p>
 * There are several predefined types of agent events, as seen in {@link AgentEventType}.
 * 
 * @author andreiolaru
 */
public class AgentEvent extends MultiValueMap
{
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 379942317804425591L;
	
	/**
	 * The enumeration specified the full set of agent-internal events that can occur.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum AgentEventType {
		
		/**
		 * Event occurs when the agent starts and its functionality (e.g. shards) need to be initialized.
		 */
		AGENT_START(AgentSequenceType.CONSTRUCTIVE),
		
		/**
		 * Event occurs when the agent must be destroyed.
		 */
		AGENT_STOP(AgentSequenceType.DESTRUCTIVE),
		
		/**
		 * Event occurs when the agent must move to a different machine.
		 */
		BEFORE_MOVE(AgentSequenceType.DESTRUCTIVE),
		
		/**
		 * Event occurs when the agent has just moved to a different machine.
		 */
		AFTER_MOVE(AgentSequenceType.CONSTRUCTIVE),
		
		/**
		 * Event occurs when the agent has received a message, an input, or another type of (generally external) event.
		 * The other parameters of the agent event are the actual parameters of the wave.
		 */
		AGENT_WAVE(AgentSequenceType.UNORDERED),
		
		/**
		 * Event occurs when the start of the simulation is requested by the user.
		 */
		SIMULATION_START(AgentSequenceType.CONSTRUCTIVE),
		
		/**
		 * Event occurs when the simulation is paused by the user.
		 */
		SIMULATION_PAUSE(AgentSequenceType.DESTRUCTIVE),
		
		;
		
		/**
		 * The sequence type that is characteristic to the event.
		 */
		protected AgentSequenceType sequenceType;
		
		/**
		 * The constructor assigns a sequence type to the event type.
		 * 
		 * @param sequence
		 *            - the sequence type, as a {@link AgentSequenceType} instance.
		 */
		private AgentEventType(AgentSequenceType sequence)
		{
			sequenceType = sequence;
		}
		
		/**
		 * @return the type of sequence associated with the event type.
		 */
		public AgentSequenceType getSequenceType()
		{
			return sequenceType;
		}
	}
	
	/**
	 * The sequence type of an agent event specifies the order in which its components (e.g. shards) should be notified
	 * of the event.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum AgentSequenceType {
		
		/**
		 * The notification should follow the order in which functionality was added to the agent.
		 */
		CONSTRUCTIVE,
		
		/**
		 * The notification should follow in reverse the order in which functionality was added to the agent.
		 */
		DESTRUCTIVE,
		
		/**
		 * The notification can be disseminated inside the agent in an arbitrary order.
		 */
		UNORDERED,
	}
	
	/**
	 * The interface should be implemented by any class that can be used as a callback for agent events.
	 * 
	 * @author Andrei Olaru
	 */
	public interface AgentEventHandler
	{
		/**
		 * The method is invoked as a callback when an appropriate event occurs.
		 * 
		 * @param event
		 *                  - the event that occurred.
		 */
		public void handleEvent(AgentEvent event);
	}
	
	/**
	 * The name of the parameter in the multi-map, storing the type of the event.
	 */
	protected static final String EVENT_TYPE_PARAMETER_NAME = "EVENT_TYPE";
	
	/**
	 * Creates a new agent event.
	 * 
	 * @param eventType
	 *            - the type of the event.
	 */
	public AgentEvent(AgentEventType eventType)
	{
		setType(eventType);
	}
	
	/**
	 * Getter for the type of the event.
	 * 
	 * @return the type of the event.
	 */
	public AgentEventType getType()
	{
		return (AgentEventType) getObject(EVENT_TYPE_PARAMETER_NAME);
	}
	
	/**
	 * Sets the type of the event.
	 * 
	 * @param eventType
	 *            - the type of the event.
	 */
	private void setType(AgentEventType eventType)
	{
		locked();
		addObject(EVENT_TYPE_PARAMETER_NAME, eventType);
	}
}
