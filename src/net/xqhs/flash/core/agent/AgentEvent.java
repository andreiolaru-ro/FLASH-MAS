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

<<<<<<< HEAD
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.util.MultiValueMap;

/**
 * The class models an agent event, characterized by its type and, optionally, a set of parameters that have names.
 * <p>
 * The class is backed by a {@link MultiValueMap}.
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
		 * Event occurs when the agent starts and the components need to be initialized.
		 */
		AGENT_START(AgentSequenceType.CONSTRUCTIVE),
		
		/**
		 * Event occurs when the agent must be destroyed and components need to close.
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
		 */
		AGENT_WAVE(AgentSequenceType.UNORDERED),
		
		/**
		 * Event occurs when the start of the simulation is requested by the user.
		 */
		SIMULATION_START(AgentSequenceType.UNORDERED),
		
		/**
		 * Event occurs when the simulation is paused by the user.
		 */
		SIMULATION_PAUSE(AgentSequenceType.UNORDERED),
		
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
	 * The sequence type of an agent event specifies the order in which components should be notified of the event.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum AgentSequenceType {
		
		/**
		 * The components should be invoked in the order they were added.
		 */
		CONSTRUCTIVE,
		
		/**
		 * The components should be invoked in inverse order as to that in which they were added.
		 */
		DESTRUCTIVE,
		
		/**
		 * The components can be invoked in any order.
		 */
		UNORDERED,
	}
	
	/**
	 * The interface should be implemented by a class that can handle agent events for an agent component. Each
	 * {@link AgentShardCore} instance is able to register, for each event, an event handler.
	 * <p>
	 * The class also contains enumerations relevant to event handling: event types ( {@link AgentEventType}) and types
	 * of sequences for events ({@link AgentSequenceType}).
	 * 
	 * @author Andrei Olaru
	 */
	public interface AgentEventHandler
	{
		/**
		 * The method is invoked whenever the event is posted to the {@link CompositeAgent} the component is part of.
		 * <p>
		 * The handlers in various components will be invoked (through the method in {@link AgentShardCore}) in the
		 * order specified by the {@link AgentSequenceType} associated with the event.
		 * 
		 * @param event
		 *            - the event that occurred.
		 */
		public void handleEvent(AgentEvent event);
	}
	
	/**
	 * The name of the parameter in the parameter set, storing the type of the event.
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
