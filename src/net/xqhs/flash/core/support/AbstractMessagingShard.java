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
package net.xqhs.flash.core.support;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Debug.DebugItem;

/**
 * Prototype class for shards offering messaging functionality, containing some general implementation-independent
 * methods.
 * 
 * <h2>Extending this class</h2> The abstract class implements some basic methods for working with slash-delimited
 * addresses, and offers the method for registering message handlers. The class also eases the task of posting agent
 * events to the agent thread, by means of the method {@link #receiveMessage} that should be called by extending
 * classes.
 * <p>
 * Basically, a class extending {@link AbstractMessagingShard} should implement the following:
 * <ul>
 * <li>if the messaging system uses agent names as their addresses, then it is recommended to extend the
 * {@link AbstractNameBasedMessagingShard} class.
 * <li>it should implement the {@link #sendMessage(String, String, String)} method that sends a message to another
 * agent.
 * <li>when a message is received, the implementation should call the {@link #receiveMessage} of this class, which will
 * pack the information into an {@link AgentEvent} instance and post it in the event queue.
 * </ul>
 * 
 * @author Andrei Olaru
 */
public abstract class AbstractMessagingShard extends AgentShardCore implements MessagingShard
{
	/**
	 * Debugging settings for messaging shards.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum MessagingDebug implements DebugItem {
		/**
		 * General messaging debugging switch.
		 */
		DEBUG_MESSAGING(true),
		
		;
		
		/**
		 * Activation state.
		 */
		boolean isset;
		
		/**
		 * Default constructor.
		 * 
		 * @param set
		 *                - activation state.
		 */
		private MessagingDebug(boolean set)
		{
			isset = set;
		}
		
		@Override
		public boolean toBool()
		{
			return isset;
		}
	}
	
	/**
	 * The serial UID.
	 */
	private static final long	serialVersionUID		= -7541956285166819418L;
	
	/**
	 * The name of the parameter in an {@link AgentEvent} associated with a message, that corresponds to the target
	 * address of the message.
	 */
	public static final String	DESTINATION_PARAMETER	= "message address";
	/**
	 * The name of the parameter in an {@link AgentEvent} associated with a message, that corresponds to the content of
	 * the message.
	 */
	public static final String	CONTENT_PARAMETER		= "message content";
	/**
	 * The name of the parameter in an {@link AgentEvent} associated with a message, that corresponds to the source of
	 * the message.
	 */
	public static final String	SOURCE_PARAMETER		= "message source";
	
	/**
	 * Default constructor.
	 */
	public AbstractMessagingShard()
	{
		super(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING));
	}
	
	/**
	 * Extending classes should call this method to defer to {@link AbstractMessagingShard} the effort of packing
	 * message data into an {@link AgentEvent} and posting that event in the agent event queue.
	 * 
	 * @param source
	 *                        - the source of the message, as a complete endpoint
	 * @param destination
	 *                        - the destination of the message, as complete endpoint (must begin with the agent's
	 *                        address).
	 * @param content
	 *                        - the content of the message
	 */
	protected void receiveMessage(String source, String destination, String content)
	{
		AgentEvent event = new AgentEvent(AgentEventType.AGENT_WAVE);
		try
		{
			event.add(AbstractMessagingShard.SOURCE_PARAMETER, source);
			event.add(AbstractMessagingShard.DESTINATION_PARAMETER, destination);
			event.add(AbstractMessagingShard.CONTENT_PARAMETER, content);
		} catch(RuntimeException e)
		{
			// should never happen.
			throw new IllegalStateException("Config locked:" + PlatformUtils.printException(e));
		}
		/**
		 * TODO: logging
		 *
		 * getAgentLog().dbg(MessagingDebug.DEBUG_MESSAGING, "Received message from [] to [] with content [].", source,
		 * destination, content);
		 */
		
		getAgent().postAgentEvent(event);
	}
	
	@Override
	public String makePath(String targetAgent, String... internalElements)
	{
		return makePathHelper(getAgentAddress(targetAgent), internalElements);
	}
	
	/**
	 * The method creates an internal path by attaching the specified elements and placing slashes between them. The
	 * result begins with a slash.
	 * <p>
	 * E.g. it produces /element1/element2/element3
	 * 
	 * @param internalElements
	 *                             - the elements in the path.
	 * @return the complete path/address.
	 */
	public static String makeInternalPath(String... internalElements)
	{
		return makePathHelper(null, internalElements);
	}
	
	@Override
	public String makeLocalPath(String... elements)
	{
		return makePathHelper(getAgentAddress(), elements);
	}
	
	/**
	 * Gets the address of the agent, as it is specific to the implementation.
	 * <p>
	 * The default implementation calls {@link #getAgentAddress(String)}.
	 * 
	 * @return the address of the parent agent, if any; <code>null</code> otherwise.
	 */
	
	public String getAgentAddress()
	{
		return getAgentAddress(getAgent().getEntityName());
	}
	
	/**
	 * The method produces the string address of an agent on the same support infrastructure, being provided with the
	 * agent name.
	 * <p>
	 * This address can subsequently be suffixed with more path elements by using {@link #makePath(String, String...)}.
	 * <p>
	 * In implementations where agents are addressed by name, the two are the same.
	 * 
	 * @param agentName
	 *                      - the name of the target agent.
	 * @return the string address of the target agent.
	 */
	public abstract String getAgentAddress(String agentName);
	
	/**
	 * The method returns the name of the agent associated with the given address.
	 * <p>
	 * In implementations where agents are addressed by name, the two are the same.
	 * 
	 * @param agentAddress
	 *                         - the address of the agent (external path).
	 * @return the name of the agent associated with the address.
	 */
	public abstract String getAgentNameFromAddress(String agentAddress);
	
	/**
	 * The method extracts, from the complete endpoint path referring to <b>this agent</b>, the elements of the path,
	 * after the address of the agent itself and also eliminating specified prefix elements.
	 * 
	 * @param event
	 *                                   - the event to extract the address from (contains a complete destination
	 *                                   endpoint). The message must be addressed to this agent.
	 * @param prefixElementsToRemove
	 *                                   - elements of the prefix to remove from the address.
	 * @return the elements that were extracted from the address, following the address of the agent and the specified
	 *         prefix elements; <code>null</code> if an error occurred (address not in the current agent or prefix
	 *         elements not part of the address in the specified order).
	 */
	public String[] extractInternalDestinationElements(AgentEvent event, String... prefixElementsToRemove)
	{
		String prefix = makeInternalPath(prefixElementsToRemove);
		String rem = extractInternalDestination(event, prefix);
		String[] ret = null;
		if(rem != null)
			ret = (rem.startsWith(ADDRESS_SEPARATOR) ? rem.substring(1) : rem).split(ADDRESS_SEPARATOR);
		return ret;
	}
	
	/**
	 * The method extracts, from the complete endpoint path referring to <b>this agent</b>, the remaining internal
	 * address, after the address of the agent itself and also eliminating a specified prefix.
	 * 
	 * @param event
	 *                           - the event to extract the address from (contains a complete destination endpoint). The
	 *                           message must be addressed to this agent.
	 * @param prefixToRemove
	 *                           - prefix to remove from the address. The prefix must be an internal path (starting with
	 *                           slash, but not ending with slash).
	 * @return the remaining internal address (starting with slash).
	 */
	
	// TODO: logging
	public String extractInternalDestination(AgentEvent event, String prefixToRemove)
	{
		if(event == null)
		{
			// if (getAgentLog() != null)
			// getAgentLog().error("Event is null.");
			return null;
		}
		String address = event.get(DESTINATION_PARAMETER);
		if(!address.startsWith(getAgentAddress()))
		{
			try
			{
				// getAgentLog().error("Address [] does not begin with this agent's address",
				// address);
			} catch(NullPointerException e)
			{
				// nothing
			}
			return null;
		}
		String rem = address.substring(getAgentAddress().length());
		if(!rem.startsWith(prefixToRemove))
		{
			try
			{
				// getAgentLog().warn("Internal path [] does not begin with the specified prefix
				// []", rem, prefixToRemove);
			} catch(NullPointerException e)
			{
				// nothing
			}
			return rem;
		}
		return rem.substring(prefixToRemove.length());
	}
	
	/**
	 * The method extracts from the message event the content of the message.
	 * 
	 * @param event
	 *                  - the event to extract the content from.
	 * @return the content of the message.
	 */
	
	// TODO: logging
	public static String extractContent(AgentEvent event)
	{
		if(event == null)
		{
			// if (getAgentLog() != null)
			// getAgentLog().error("Event is null.");
			return null;
		}
		String content = event.get(CONTENT_PARAMETER);
		return content;
	}
	
	/**
	 * The method extracts, from a complete endpoint address, the external path, which is the address of the agent
	 * identified by the endpoint.
	 * 
	 * @param endpoint
	 *                     - the complete endpoint address.
	 * @return the external path, or address of the agent.
	 */
	public abstract String extractAgentAddress(String endpoint);
	
	/**
	 * The method extracts, from a complete endpoint address, the internal path. It should always begin with a slash.
	 * <p>
	 * The default implementation of the method uses {@link #extractAgentAddress(String)} to identify the external path
	 * in the endpoint. Specific implementations may do this more efficiently.
	 * 
	 * @param endpoint
	 *                     - the complete endpoint address.
	 * @return the internal part of the endpoint address.
	 */
	public String extractInternalAddress(String endpoint)
	{
		String externalPath = extractAgentAddress(endpoint);
		if(!endpoint.startsWith(externalPath))
			throw new IllegalStateException("Endpoint address does not start with agent address");
		return endpoint.substring(externalPath.length());
	}
	
	/**
	 * Parses the result given by {@link #extractInternalAddress(String)} on the given endpoint and separates the
	 * elements of the internal address (according to the {@link #ADDRESS_SEPARATOR}={@value #ADDRESS_SEPARATOR}).
	 * 
	 * @param endpoint
	 *                     - the complete endpoint address.
	 * @return the separate elements of the internal path.
	 */
	public String[] extractInternalAddressElements(String endpoint)
	{
		String internalPath = extractInternalAddress(endpoint);
		return (internalPath.startsWith(ADDRESS_SEPARATOR) ? internalPath.substring(1) : internalPath)
				.split(ADDRESS_SEPARATOR);
	}
	
	@Override
	public abstract boolean sendMessage(String target, String source, String content);
}
