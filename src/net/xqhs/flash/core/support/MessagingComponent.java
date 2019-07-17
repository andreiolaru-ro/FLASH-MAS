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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.core.composite.AgentEvent;
import net.xqhs.flash.core.composite.AgentEvent.AgentEventHandler;
import net.xqhs.flash.core.composite.AgentEvent.AgentEventType;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Debug.DebugItem;

/**
 * The messaging component should handle all communication between the agent and other agents. Note that the existence
 * of such a component in the {@link CompositeAgent} modifies its behavior: without any messaging component, received
 * messages are notified as agent events to all components of the agent; with it, messages are passed to the messaging
 * component, which then should route them to the appropriate receivers.
 * 
 * <h2>Endpoints</h2> The communication is abstracted internally to the agent as exchanging messages between endpoints,
 * where each endpoint is identified by a {@link String} address composed of multiple elements separated by slashes (by
 * default {@link #ADDRESS_SEPARATOR}={@value #ADDRESS_SEPARATOR}). This means that an endpoint may be identified by an
 * URI (if the {@link MessagingComponent} implementation supports it), or, for instance, by an address of the type
 * "Agent1/Visualization" (this will work with Jade, where agents are addressable by name).
 * <p>
 * Messages are sent from one endpoint to another, and contain a {@link String}. Specific implementations may parse the
 * string for additional structure.
 * <p>
 * We make a difference between [complete] endpoints (or paths) and internal endpoints (or paths).
 * <p>
 * <b>IMPORTANT NOTE:</b> An agent address (returned by {@link #getAgentAddress}) concatenated with an internal path
 * should result in a complete path. Internal paths should begin with a slash. Otherwise, the predefined methods for
 * parsing and assembling endpoint paths may not work correctly.
 * 
 * <h2>Using messaging features</h2> In principle, other components should not need to call the [implementation of the]
 * MessagingComponent explicitly. Functionality of the messaging component should be used by means of the protected
 * methods in {@link AgentComponent}, such as <code>registerMessageReceiver</code>, <code>sendMessage</code> and
 * <code>getComponentEndpoint</code>. Messages will be signaled, as they are processed from the agent's event queue, by
 * means of the specified receivers.
 * 
 * <h2>Extending this class</h2> The abstract class implements some basic methods for working with slash-delimited
 * addresses, and offers the method for registering message handlers. The class also eases the task of posting agent
 * events to the agent thread, by means of the method {@link #receiveMessage} that should be called by extending
 * classes.
 * <p>
 * Basically, a class extending {@link MessagingComponent} should implement the following:
 * <ul>
 * <li>if the messaging system uses agent names as their addresses, then it is recommended to extend the
 * {@link NameBasedMessagingComponent} class.
 * <li>if it needs to access the platform containing the agent and implementing communication, it should do that after
 * {@link #atAgentStart} is called (at any point before this method is called the agent may not be loaded onto the
 * platform yet). To get a reference to the platform, by means of the {@link #getPlatformLink} method.
 * <li>it should implement the {@link #sendMessage(String, String, String)} method that sends a message to another
 * agent.
 * <li>when a message is received, the implementation should call the {@link #receiveMessage} of the parent, that will
 * pack the information into an {@link AgentEvent} instance and post it in the event queue. After the event is picked
 * from the event queue, {@link MessagingComponent} will use the <code>handleMessage()</code> method to distribute the
 * message to receiving components.
 * </ul>
 * 
 * 
 * @author Andrei Olaru
 */
public abstract class MessagingComponent extends AgentShardCore
{
	/**
	 * Debugging settings for messaging components.
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
		 *            - activation state.
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
	private static final long serialVersionUID = -7541956285166819418L;
	
	/**
	 * The string separating elements of an endpoint address.
	 */
	public static final String	ADDRESS_SEPARATOR		= "/";
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
	 * The {@link Map} of {@link AgentEventHandler} instances that were registered with this component, associated with
	 * their respective endpoints. Multiple handlers may be registered with the same endpoint. These handlers will be
	 * invoked in no particular order. The endpoint is an internal path, rather than a complete path.
	 */
	protected Map<String, Set<AgentEventHandler>> messageHandlers = new HashMap<String, Set<AgentEventHandler>>();
	
	/**
	 * Default constructor.
	 */
	public MessagingComponent()
	{
		AgentShardDesignation.standardFeature(StandardAgentShard.MESSAGING);
		
		registerHandler(AgentEventType.AGENT_MESSAGE, new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				handleMessage(event);
			}
		});
	}
	
	/**
	 * Extending classes should call this method to defer to {@link MessagingComponent} the effort of packing message
	 * data into an {@link AgentEvent} and posting that event in the agent event queue.
	 * 
	 * @param source
	 *            - the source of the message, as a complete endpoint
	 * @param destination
	 *            - the destination of the message, as complete endpoint (must begin with the agent's address).
	 * @param content
	 *            - the content of the message
	 */
	protected void receiveMessage(String source, String destination, String content)
	{
		AgentEvent event = new AgentEvent(AgentEventType.AGENT_MESSAGE);
		try
		{
			event.add(MessagingComponent.SOURCE_PARAMETER, source);
			event.add(MessagingComponent.DESTINATION_PARAMETER, destination);
			event.add(MessagingComponent.CONTENT_PARAMETER, content);
		} catch(RuntimeException e)
		{
			// should never happen.
			throw new IllegalStateException("Config locked:" + PlatformUtils.printException(e));
		}
		getAgentLog().dbg(MessagingDebug.DEBUG_MESSAGING, "Received message from [] to [] with content [].", source,
				destination, content);
		
		postAgentEvent(event);
	}
	
	/**
	 * Handles a message received by the agent. This method should be overridden for specific implementations.
	 * <p>
	 * The method dispatches the message to handlers that have registered for the target endpoint (or for endpoints
	 * including (are prefixes of) the target endpoint), by means of
	 * {@link #registerMessageReceiver(AgentEventHandler, String...)} or <code>registerMessageReceiver()</code> in
	 * {@link AgentComponent}.
	 * 
	 * @param event
	 *            - the event corresponding to the message.
	 */
	protected void handleMessage(AgentEvent event)
	{
		String destinationInternal = extractInternalDestination(event, "");
		if(destinationInternal == null)
		{
			if(getAgentLog() != null)
				getAgentLog().error("No internal destination returned for event", event);
			return;
		}
		if(destinationInternal.length() == 0)
			// if no internal path, make it the root path (a correct internal path).
			destinationInternal = "/";
		for(Map.Entry<String, Set<AgentEventHandler>> entry : messageHandlers.entrySet())
		{
			getAgentLog().dbg(MessagingDebug.DEBUG_MESSAGING, "Comparing: [] to declared []", destinationInternal,
					entry.getKey());
			if(destinationInternal.startsWith(entry.getKey()))
				// prefix matches
				for(AgentEventHandler receiver : entry.getValue())
				{
					getAgentLog().dbg(MessagingDebug.DEBUG_MESSAGING, "Dispatching to []", receiver);
					receiver.handleEvent(event);
				}
		}
	}
	
	/**
	 * Registers a new message receiver for the specified prefix (internal path). Messages to a target beginning with
	 * the prefix (after the prefix of the agent has been removed) will be delivered to this handler.
	 * <p>
	 * Multiple handlers may be registered for the same prefix.
	 * <p>
	 * WARNING: this implementation should not be confused with the method it overrides from {@link AgentComponent}.
	 * This is the method that actually registers the receiver with the messaging component. In other components (that
	 * do not extend {@link MessagingComponent}), the method will be used to relay calls either to the method in the
	 * {@link MessagingComponent} implementation or to the {@link CompositeAgent} instance.
	 * 
	 * @param receiver
	 *            - the message receiver, as an {@link AgentEventHandler} instance.
	 * @param prefixElements
	 *            - the prefix of the target, as elements of the internal path.
	 * @return always <code>true</code>, according to the meaning of the return value as given in the overridden method.
	 */
	@Override
	protected boolean registerMessageReceiver(AgentEventHandler receiver, String... prefixElements)
	{
		String prefix = makeInternalPath(prefixElements);
		if(!messageHandlers.containsKey(prefix))
			messageHandlers.put(prefix, new HashSet<AgentEventHandler>());
		messageHandlers.get(prefix).add(receiver);
		return true;
	}
	
	/**
	 * The method creates a complete path by attaching the specified elements and placing slashes between them.
	 * <p>
	 * E.g. it produces targetAgent/element1/element2/element3
	 * 
	 * @param targetAgent
	 *            - the name of the searched agent.
	 * @param internalElements
	 *            - the elements in the internal path.
	 * @return the complete path/address.
	 */
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
	 *            - the elements in the path.
	 * @return the complete path/address.
	 */
	@SuppressWarnings("static-method")
	public String makeInternalPath(String... internalElements)
	{
		return makePathHelper(null, internalElements);
	}
	
	/**
	 * The method creates a complete path by attaching the specified elements to the address of this agent.
	 * <p>
	 * E.g. it produces thisAgent/element1/element2/element3
	 * 
	 * @param elements
	 *            - the elements in the path.
	 * @return the complete path/address.
	 */
	public String makeLocalPath(String... elements)
	{
		return makePathHelper(getAgentAddress(), elements);
	}
	
	/**
	 * Produces an address by assembling the start of the address with the rest of the elements. They will be separated
	 * by the address separator specified as constant.
	 * <p>
	 * Elements that are <code>null</code> will not be assembled in the path.
	 * <p>
	 * If the start is <code>null</code> the result will begin with a slash.
	 * 
	 * @param start
	 *            - start of the address.
	 * @param elements
	 *            - other elements in the address
	 * @return the resulting address.
	 */
	public static String makePathHelper(String start, String... elements)
	{
		String ret = (start != null) ? start : "";
		for(String elem : elements)
			if(elem != null)
				ret += ADDRESS_SEPARATOR + elem;
		return ret;
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
		return getAgentAddress(getAgentName());
	}
	
	/**
	 * The method produces the string address of an agent on the same platform, being provided with the agent name.
	 * <p>
	 * This address can subsequently be suffixed with more path elements by using {@link #makePath(String, String...)}.
	 * <p>
	 * In implementations where agents are addressed by name, the two are the same.
	 * 
	 * @param agentName
	 *            - the name of the target agent.
	 * @return the string address of the target agent.
	 */
	public abstract String getAgentAddress(String agentName);
	
	/**
	 * The method returns the name of the agent associated with the given address.
	 * <p>
	 * In implementations where agents are addressed by name, the two are the same.
	 * 
	 * @param agentAddress
	 *            - the address of the agent (external path).
	 * @return the name of the agent associated with the address.
	 */
	public abstract String getAgentNameFromAddress(String agentAddress);
	
	/**
	 * The method extracts, from the complete endpoint path referring to <b>this agent</b>, the elements of the path,
	 * after the address of the agent itself and also eliminating specified prefix elements.
	 * 
	 * @param event
	 *            - the event to extract the address from (contains a complete destination endpoint). The message must
	 *            be addressed to this agent.
	 * @param prefixElementsToRemove
	 *            - elements of the prefix to remove from the address.
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
	 *            - the event to extract the address from (contains a complete destination endpoint). The message must
	 *            be addressed to this agent.
	 * @param prefixToRemove
	 *            - prefix to remove from the address. The prefix must be an internal path (starting with slash, but not
	 *            ending with slash).
	 * @return the remaining internal address (starting with slash).
	 */
	public String extractInternalDestination(AgentEvent event, String prefixToRemove)
	{
		if(event == null)
		{
			if(getAgentLog() != null)
				getAgentLog().error("Event is null.");
			return null;
		}
		String address = event.get(DESTINATION_PARAMETER);
		if(!address.startsWith(getAgentAddress()))
		{
			try
			{
				getAgentLog().error("Address [] does not begin with this agent's address", address);
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
				getAgentLog().warn("Internal path [] does not begin with the specified prefix []", rem, prefixToRemove);
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
	 *            - the event to extract the content from.
	 * @return the content of the message.
	 */
	public String extractContent(AgentEvent event)
	{
		if(event == null)
		{
			if(getAgentLog() != null)
				getAgentLog().error("Event is null.");
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
	 *            - the complete endpoint address.
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
	 *            - the complete endpoint address.
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
	 *            - the complete endpoint address.
	 * @return the separate elements of the internal path.
	 */
	public String[] extractInternalAddressElements(String endpoint)
	{
		String internalPath = extractInternalAddress(endpoint);
		return (internalPath.startsWith(ADDRESS_SEPARATOR) ? internalPath.substring(1) : internalPath)
				.split(ADDRESS_SEPARATOR);
	}
	
	/**
	 * Sends a message to another agent, according to the specific implementation.
	 * 
	 * @param target
	 *            - the target (complete) endpoint of the message.
	 * @param source
	 *            - the source (internal) endpoint of the message.
	 * @param content
	 *            - the content of the message.
	 * @return <code>true</code> if the message was sent successfully.
	 */
	public abstract boolean sendMessage(String target, String source, String content);
}
