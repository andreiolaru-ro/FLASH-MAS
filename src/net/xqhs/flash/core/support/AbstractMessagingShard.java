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
package net.xqhs.flash.core.support;

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.util.logging.Debug.DebugItem;

/**
 * Prototype class for shards offering messaging functionality, containing some general implementation-independent
 * methods. it also manages the relation with the {@link MessagingPylonProxy}.
 * 
 * <h2>Extending this class</h2> The abstract class implements some basic methods for working with slash-delimited
 * addresses, and offers the method for registering message handlers. The class also eases the task of posting agent
 * events to the agent thread, by means of the method {@link #receiveMessage} that should be called by extending
 * classes.
 * <p>
 * Basically, a class extending {@link AbstractMessagingShard} should implement the following:
 * <ul>
 * <li>if the messaging system uses agent names (as opposed to addresses with slashes) as their addresses, then it is
 * recommended to extend the {@link NameBasedMessagingShard} class. agent.
 * <li>it should implement the {@link #extractAgentAddress(String)} which extracts the address of the agent from a
 * complete endpoint.
 * <li>it should implement the {@link #getAgentAddress()} which retrieves the address of the current agent. pack the
 * information into an {@link AgentWave} and post it in the event queue.
 * <li>any other functionality that works differently than this standard implementation.
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
	private static final long serialVersionUID = -7541956285166819418L;
	
	/**
	 * Endpoint name for this shard (see {@link AgentWave}).
	 */
	protected static final String		SHARD_ENDPOINT		= "messaging";
	
	/**
	 * Reference to the local Websocket pylon.
	 */
	transient protected MessagingPylonProxy	pylon	= null;
	/**
	 * The proxy to this shard, to be used by the pylon.
	 */
	protected transient MessageReceiver		inbox	= null;
	/**
	 * Outgoing message hooks.
	 */
	protected transient Set<OutgoingMessageHook>	outgoingHooks	= new HashSet<>();
	
	/**
	 * No-argument constructor.
	 */
	public AbstractMessagingShard()
	{
		super(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING));
	}
	
	/**
	 * @return a standard {@link MessageReceiver} implementation which relays all received messages to the method in
	 *         {@link AbstractMessagingShard}.
	 */
	protected MessageReceiver buildMessageReceiver() {
		return new MessageReceiver() {
			@Override
			public void receive(String source, String destination, String content) {
				receiveMessage(source, destination, content);
			}
		};
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!(context instanceof MessagingPylonProxy)) {
			return false;
		}
		if(inbox == null)
			inbox = buildMessageReceiver();
		pylon = (MessagingPylonProxy) context;
		return true;
	}
	
	@Override
	public void register(String entityName) {
		pylon.register(entityName, inbox);
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			if(pylon == null)
				throw new IllegalStateException("Shard is not currently added within a pylon");
			pylon.register(getAgent().getEntityName(), inbox);
			break;
		case AGENT_STOP:
			if(pylon != null)
				pylon.unregister(getAgent().getEntityName(), inbox);
			break;
		default:
			break;
		}
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
		String localAddr = getAgentAddress();
		if(!destination.startsWith(localAddr))
			throw new IllegalStateException("Destination endpoint (" + destination
					+ ") does not start with the address of this agent (" + localAddr + ")");
		
		AgentWave wave = new AgentWave(content, localAddr, AgentWave.pathToElements(destination, localAddr));
		// already routed to this agent
		wave.removeFirstDestinationElement();
		
		String senderAddr = extractAgentAddress(source);
		if(!source.startsWith(senderAddr))
			// FIXME use log
			throw new IllegalStateException("Source endpoint (" + source
					+ ") does not start with the address of this agent (" + senderAddr + ")");
		wave.addSourceElements(AgentWave.pathToElementsWith(source, senderAddr));
		
		/**
		 * TODO: logging
		 *
		 * getAgentLog().dbg(MessagingDebug.DEBUG_MESSAGING, "Received message from [] to [] with content [].", source,
		 * destination, content);
		 */
		
		getAgent().postAgentEvent(wave);
	}
	
	@Override
	public boolean sendMessage(String source, String target, String content) {
		String fullsource = source.startsWith(getAgentAddress()) ? source
				: AgentWave.makePath(getAgentAddress(), source);
		for(OutgoingMessageHook hook : outgoingHooks)
			hook.sendingMessage(fullsource, target, content);
		return pylon.send(fullsource, target, content);
	}

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
	
	@Override
	public void addOutgoingMessageHook(OutgoingMessageHook hook) {
		outgoingHooks.add(hook);
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
	
	@Override
	public abstract String getAgentAddress();
}
