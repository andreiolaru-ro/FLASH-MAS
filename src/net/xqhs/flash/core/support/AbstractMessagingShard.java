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

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.util.logging.Debug.DebugItem;

/**
 * Prototype class for shards offering messaging functionality, containing some general implementation-independent
 * methods. It also manages the relation with the {@link MessagingPylonProxy}.
 * 
 * <h2>Extending this class</h2> The abstract class implements some basic methods for working with slash-delimited
 * addresses, and offers the method for registering message handlers. The class also eases the task of posting agent
 * events to the agent thread, by means of the method {@link #receiveMessage} that should be called by extending
 * classes.
 * <p>
 * Basically, a class extending {@link AbstractMessagingShard} should implement the following:
 * <ul>
 * <li>if the messaging system uses agent names (as opposed to addresses with slashes) as their addresses, then it is
 * recommended to extend the {@link NameBasedMessagingShard} class.
 * <li>it should implement the {@link #extractAgentAddress(String)} which extracts the address of the agent from a
 * complete endpoint.
 * <li>it should implement the {@link #getAgentAddress()} which retrieves the address of the current agent.
 * <li>any other functionality that works differently than this standard implementation.
 * </ul>
 * 
 * @author Andrei Olaru
 */
public abstract class AbstractMessagingShard extends AgentShardCore implements MessagingShard {
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
		 *            - activation state.
		 */
		private MessagingDebug(boolean set) {
			isset = set;
		}
		
		@Override
		public boolean toBool() {
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
	protected static final String SHARD_ENDPOINT = "messaging";
	
	/**
	 * Reference to the pylon, if the pylon does not support wave messaging. At most one of {@link #classicPylon} and
	 * {@link #wavePylon} can be not <code>null</code>.
	 */
	transient protected ClassicMessagingPylonProxy	classicPylon	= null;
	/**
	 * Reference to the pylon, if the pylon supports wave messaging. At most one of {@link #classicPylon} and
	 * {@link #wavePylon} can be not <code>null</code>.
	 */
	transient protected WaveMessagingPylonProxy		wavePylon		= null;
	/**
	 * The proxy to this shard, to be used by the pylon, if the pylon does not support wave messaging. At most one of
	 * {@link #classicInbox} and {@link #waveInbox} can be not <code>null</code>.
	 */
	protected transient ClassicMessageReceiver		classicInbox	= null;
	/**
	 * The proxy to this shard, to be used by the pylon, if the pylon supports wave messaging. At most one of
	 * {@link #classicInbox} and {@link #waveInbox} can be not <code>null</code>.
	 */
	protected transient WaveReceiver				waveInbox		= null;
	
	/**
	 * No-argument constructor.
	 */
	public AbstractMessagingShard() {
		super(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(context instanceof ClassicMessagingPylonProxy) {
			classicPylon = (ClassicMessagingPylonProxy) context;
			classicInbox = new ClassicMessageReceiver() {
				@Override
				public void receive(String source, String destination, String content) {
					receiveMessage(source, destination, content);
				}
			};
			wavePylon = null;
			waveInbox = null;
			return true;
		}
		else if(context instanceof WaveMessagingPylonProxy) {
			wavePylon = (WaveMessagingPylonProxy) context;
			waveInbox = new WaveReceiver() {
				@Override
				public void receive(AgentWave wave) {
					receiveWave(wave);
				}
			};
			classicPylon = null;
			classicInbox = null;
			return true;
		}
		else
			return false;
	}
	
	@Override
	public void register(String entityName) {
		if(classicPylon != null)
			classicPylon.register(entityName, classicInbox);
		if(wavePylon != null)
			wavePylon.register(entityName, waveInbox);
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			if(classicPylon == null && wavePylon == null)
				throw new IllegalStateException("Shard is not currently added within a pylon");
			if(classicPylon != null)
				classicPylon.register(getAgent().getEntityName(), classicInbox);
			else
				wavePylon.register(getAgent().getEntityName(), waveInbox);
			break;
		case AGENT_STOP:
			if(classicPylon != null)
				classicPylon.unregister(getAgent().getEntityName(), classicInbox);
			if(wavePylon != null)
				wavePylon.unregister(getAgent().getEntityName(), waveInbox);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Extending classes should call this method to defer to {@link AbstractMessagingShard} the effort of packing
	 * message data into an {@link AgentEvent} and posting that event in the agent event queue. It calls
	 * {@link #receiveWave(AgentWave)} for necessary checks and posting the agent event.
	 * 
	 * @param source
	 *            - the source of the message, as a complete endpoint
	 * @param destination
	 *            - the destination of the message, as complete endpoint (must begin with the agent's address).
	 * @param content
	 *            - the content of the message
	 */
	protected void receiveMessage(String source, String destination, String content) {
		AgentWave wave = new AgentWave(content).appendDestination(AgentWave.pathToElements(destination, null))
				.addSourceElements(AgentWave.pathToElementsWith(source, null));
		receiveWave(wave);
	}
	
	/**
	 * Extending classes should call this method to check a received wave.
	 * <p>
	 * Only the {@link AgentWave#DESTINATION_ELEMENT} key will be considered for destination information.
	 * <p>
	 * Checks are:
	 * <ul>
	 * the first destination element is this agent.
	 * <li>
	 * </ul>
	 * 
	 * @param wave
	 *            - the received wave.
	 */
	protected void receiveWave(AgentWave wave) {
		if(!getAgentAddress().equals(wave.getFirstDestinationElement()))
			throw new IllegalStateException(
					"The first element in destination endpoint (" + wave.getValues(AgentWave.DESTINATION_ELEMENT)
							+ ") is not the address of this agent (" + getAgentAddress() + ")");
		
		// already routed to this agent
		wave.removeFirstDestinationElement();
		
		if(!wave.getCompleteSource().startsWith(extractAgentAddress(wave.getCompleteSource())))
			// FIXME use log
			throw new IllegalStateException(
					"Source endpoint (" + wave.getCompleteSource() + ") does not start with the address of this agent ("
							+ extractAgentAddress(wave.getCompleteSource()) + ")");
		
		/**
		 * TODO: logging
		 *
		 * getAgentLog().dbg(MessagingDebug.DEBUG_MESSAGING, "Received message from [] to [] with content [].", source,
		 * destination, content);
		 */
		getAgent().postAgentEvent(wave);
	}
	
	@Override
	public boolean sendMessage(String source, String destination, String content) {
		if(classicPylon != null)
			return classicPylon.send(source, destination, content);
		else if(wavePylon != null) {
			return wavePylon.send(new AgentWave(content).appendDestination(AgentWave.pathToElements(destination))
					.addSourceElements(AgentWave.pathToElementsPlus(source, getAgentAddress())));
		}
		else
			return false;
	}
	
	@Override
	public boolean sendMessage(AgentWave wave) {
		if(!getAgentAddress().equals(wave.getFirstSource()))
			wave.addSourceElementFirst(getAgentAddress());
		if(wavePylon != null)
			return wavePylon.send(wave);
		else if(classicPylon != null)
			return classicPylon.send(wave.getCompleteSource(), wave.getCompleteDestination(),
					wave.getSerializedContent());
		else
			return false;
	}
	
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
	public String extractInternalAddress(String endpoint) {
		String externalPath = extractAgentAddress(endpoint);
		if(!endpoint.startsWith(externalPath))
			throw new IllegalStateException("Endpoint address does not start with agent address");
		return endpoint.substring(externalPath.length());
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
	
	@Override
	public abstract String getAgentAddress();
}
