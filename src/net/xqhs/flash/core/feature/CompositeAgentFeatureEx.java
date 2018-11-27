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
package net.xqhs.flash.core.feature;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.agent.composite.AgentEvent;
import net.xqhs.flash.core.agent.composite.AgentEvent.AgentEventHandler;
import net.xqhs.flash.core.agent.composite.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation;
import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation.StandardAgentFeature;
import net.xqhs.flash.core.agent.composite.CompositeAgent;
import net.xqhs.flash.core.agent.composite.CompositeAgentFeature;
import net.xqhs.flash.core.agent.composite.VisualizableFeature;
import net.xqhs.flash.core.support.MessagingComponent;
import net.xqhs.flash.core.support.MessagingFeature;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.logging.DumbLogger;
import net.xqhs.util.logging.Logger;

/**
 * This class extends on {@link CompositeAgentFeature} by adding some functionality which may be useful to many agent
 * feature implementations. These methods are <code>protected</code>, as they should be accessible only from the inside
 * of the feature, not from the outside. These methods are enumerated below:
 * <ul>
 * <li>Methods that are called when events happen. These methods should be overridden by extending classes in order to
 * intercept initialization (constructor, <code>featureInitializer()</code>, {@link #configure}), parent change (
 * <code>parentChangeNotifier()</code>), and other events. The class contains methods that, <i>by default</i>, are
 * registered as event handlers for some agent events (agent start/stop, simulation start/pause, before/after move).
 * Other handlers may be registered instead by the implementation, so these methods ( <code>at*()</code>) are not
 * guaranteed to be called when the event occurs. It is the choice of the developer of the extending class if events are
 * to be handled by overriding these methods or by registering new handlers. There are events for which default handlers
 * are not registered (e.g. <code>AGENT_MESSAGE</code>).
 * 
 * <li>Methods to access feature creation data: <code>getFeatureDesignation()</code> and <code>getFeatureData()</code>.
 * 
 * <li>Methods that relay to access some package-accessible functionality from {@link CompositeAgent}:
 * <code>getAgentName()</code>, <code>getParent()</code>, <code>getPlatformLink()</code> -- retrieves an {@link Object}
 * that can be casted to the specific implementation of what the platform offers in order to access platform-specific
 * functionality; <code>getAgentFeature()</code> -- retrieve an agent feature by designation.
 * 
 * <li>Methods for event handling: <code>postAgentEvent()</code> to post a new agent event to the agent's event queue;
 * and <code>registerHandler()</code> to register handlers for agent events. These handlers are the handlers associated
 * to <i>this</i> feature.
 * 
 * <li>Functionality specific to other commonly-used features:
 * <ul>
 * 
 * <li>visualization: the method <code>getAgentLog()</code> retrieves the {@link Logger} implementation used by the
 * agent, relying the call to the {@link VisualizableFeature} of the agent (if any).
 * 
 * <li>messaging: the methods <code>registerMessageReceiver</code>, <code>sendMessage</code> and
 * <code>getFeatureEndpoint</code> call methods in the {@link MessagingFeature} of the agent (if any) to register a
 * handler for messages sent to a particular internal endpoint; to send a message; or to compute the endpoint of
 * <i>this</i> feature.
 * </ul>
 * </ul>
 * <p>
 * TODO: implement mechanisms to enable caching of feature references, invalidating the cache only at specific agent
 * events; add the event FEATURE_CHANGE.
 * <p>
 * 
 * @author andreiolaru
 */
public abstract class CompositeAgentFeatureEx extends CompositeAgentFeature
{
	
	/**
	 * The serial UID
	 */
	private static final long						serialVersionUID	= -3735312887231331007L;
	
	/**
	 * The {@link AgentEventHandler} instances that respond to various events in the agent.
	 */
	private Map<AgentEventType, AgentEventHandler>	eventHandlers;
	
	/**
	 * The constructor assigns the designation to the feature.
	 * <p>
	 * IMPORTANT: see {@link CompositeAgentFeature#AgentFeature(AgentFeatureDesignation)}.
	 * 
	 * @param designation
	 *            - the designation of the feature, as instance of {@link StandardAgentFeature}.
	 */
	public CompositeAgentFeatureEx(AgentFeatureDesignation designation)
	{
		super(designation);
	}
	
	@Override
	public boolean configure(TreeParameterSet parameters)
	{
		if(!super.configure(parameters))
			return false;
		
		// register usual events
		eventHandlers = new HashMap<>();
		registerHandler(AgentEventType.AGENT_START, new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				atAgentStart(event);
			}
		});
		registerHandler(AgentEventType.SIMULATION_START, new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				atSimulationStart(event);
			}
		});
		registerHandler(AgentEventType.SIMULATION_PAUSE, new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				atSimulationPause(event);
			}
		});
		registerHandler(AgentEventType.AGENT_STOP, new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				atAgentStop(event);
			}
		});
		registerHandler(AgentEventType.BEFORE_MOVE, new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				atBeforeAgentMove(event);
			}
		});
		registerHandler(AgentEventType.AFTER_MOVE, new AgentEventHandler() {
			@Override
			public void handleEvent(AgentEvent event)
			{
				atAfterAgentMove(event);
			}
		});
		return true;
	}
	
	/**
	 * Method that is called by the default handler for {@link AgentEventType#AGENT_START}.
	 * <p>
	 * Extending classes should override this method and should consider calling the overridden method first.
	 * <p>
	 * Since extending classes may register other handlers for the event, it is not guaranteed that this method will be
	 * called when the event occurs.
	 * 
	 * @param event
	 *            - the event that occurred.
	 */
	protected void atAgentStart(AgentEvent event)
	{
		// this class does not do anything here.
	}
	
	/**
	 * Method that is called by the default handler for {@link AgentEventType#SIMULATION_START}.
	 * <p>
	 * Extending classes should override this method and should consider calling the overridden method first.
	 * <p>
	 * Since extending classes may register other handlers for the event, it is not guaranteed that this method will be
	 * called when the event occurs.
	 * 
	 * @param event
	 *            - the event that occurred.
	 */
	protected void atSimulationStart(AgentEvent event)
	{
		// this class does not do anything here.
	}
	
	/**
	 * Method that is called by the default handler for {@link AgentEventType#SIMULATION_PAUSE}.
	 * <p>
	 * Extending classes should override this method and should consider calling the overridden method first.
	 * <p>
	 * Since extending classes may register other handlers for the event, it is not guaranteed that this method will be
	 * called when the event occurs.
	 * 
	 * @param event
	 *            - the event that occurred.
	 */
	protected void atSimulationPause(AgentEvent event)
	{
		// this class does not do anything here.
	}
	
	/**
	 * Method that is called by the default handler for {@link AgentEventType#AGENT_STOP}.
	 * <p>
	 * Extending classes should override this method and should consider calling the overridden method first.
	 * <p>
	 * Since extending classes may register other handlers for the event, it is not guaranteed that this method will be
	 * called when the event occurs.
	 * 
	 * @param event
	 *            - the event that occurred.
	 */
	protected void atAgentStop(AgentEvent event)
	{
		// this class does not do anything here.
	}
	
	/**
	 * Method that is called by the default handler for {@link AgentEventType#BEFORE_MOVE}.
	 * <p>
	 * Extending classes should override this method and should consider calling the overridden method first.
	 * <p>
	 * Since extending classes may register other handlers for the event, it is not guaranteed that this method will be
	 * called when the event occurs.
	 * 
	 * @param event
	 *            - the event that occurred.
	 */
	protected void atBeforeAgentMove(AgentEvent event)
	{
		// this class does not do anything here.
	}
	
	/**
	 * Method that is called by the default handler for {@link AgentEventType#AFTER_MOVE}.
	 * <p>
	 * Extending classes should override this method and should consider calling the overridden method first.
	 * <p>
	 * Since extending classes may register other handlers for the event, it is not guaranteed that this method will be
	 * called when the event occurs.
	 * 
	 * @param event
	 *            - the event that occurred.
	 */
	protected void atAfterAgentMove(AgentEvent event)
	{
		// this class does not do anything here.
	}
	
	/**
	 * The method calls the event handler of the feature for the event which occurred.
	 * <p>
	 * It relays the call from the parent {@link CompositeAgent}.
	 * 
	 * @param event
	 *            - the event which occurred.
	 */
	@Override
	void signalAgentEvent(AgentEvent event)
	{
		if(eventHandlers.containsKey(event.getType()))
			eventHandlers.get(event.getType()).handleEvent(event);
	}
	
	/**
	 * Relay for calls to the method {@link CompositeAgent#getName()}.
	 * 
	 * @return the name of the agent, or <code>null</code> if the feature has no parent.
	 */
	protected String getAgentName()
	{
		return (getAgent() != null) ? getAgent().getName() : null;
	}
	
	/**
	 * Relay for calls to the method {@link CompositeAgent#getSupportImplementation()}.
	 * 
	 * @return the platform link.
	 */
	protected Object getSupportImplementation()
	{
		return getAgent().getSupportImplementation();
	}
	
	/**
	 * Relay for calls to the method {@link CompositeAgent#getFeature(StandardAgentFeature)}.
	 * 
	 * @param designation
	 *            - the designation of the feature.
	 * @return the {@link CompositeAgentFeature} instance, if any. <code>null</code> otherwise.
	 */
	protected CompositeAgentFeature getAgentFeature(AgentFeatureDesignation designation)
	{
		return (getAgent() != null) ? getAgent().getFeature(designation) : null;
	}
	
	/**
	 * Extending classes should use this method to register {@link AgentEventHandler} instances that would be invoked
	 * when the specified {@link AgentEventType} appears.
	 * <p>
	 * Important note: The registered handler is the handler specific to <b>this</b> feature (therefore it is sufficient
	 * to have only one). Each feature of the agent may contain (at most) a handler for the same event.
	 * <p>
	 * Should a handler for the same event already exist, the old handler will be discarded. A reference to it will be
	 * returned.
	 * 
	 * @param event
	 *            - the agent event to be handled, as an {@link AgentEventType} instance.
	 * @param handler
	 *            - the {@link AgentEventHandler} instance to handle the event.
	 * @return the handler being replaced, if any (<code>null</code> otherwise).
	 */
	protected AgentEventHandler registerHandler(AgentEventType event, AgentEventHandler handler)
	{
		AgentEventHandler oldHandler = null;
		if(eventHandlers.containsKey(event))
		{
			oldHandler = eventHandlers.get(event);
			getAgentLog().warn("Handler for event [] overwritten with []; was []", event, handler, oldHandler);
		}
		eventHandlers.put(event, handler);
		return oldHandler;
	}
	
	/**
	 * @return the log of the agent, or an instance of {@link DumbLogger}, if one is not present or cannot be obtained.
	 *         <code>null</code> should never be returned.
	 */
	protected Logger getAgentLog()
	{
		try
		{
			return ((VisualizableFeature) getAgentFeature(StandardAgentFeature.VISUALIZABLE)).getLog();
		} catch(NullPointerException e)
		{
			return DumbLogger.get();
		}
	}
	
	/**
	 * Handles the registration of an event handler for messages to a target (inside the agent) with the specified
	 * prefix (elements of the internal path of the endpoint).
	 * <p>
	 * If the feature has a parent and a {@link MessagingFeature} exists, the handler will be registered with the
	 * messaging feature. Otherwise, the handler be registered to receive any messages, regardless of prefix. In the
	 * latter case, <code>false</code> will be returned to signal the abnormal behavior.
	 * 
	 * @param receiver
	 *            - the receiving {@link AgentEventHandler} instance.
	 * @param prefixElements
	 *            - the target prefix, as separate elements of the internal path.
	 * @return <code>true</code> if the registration was successful; <code>false</code> if the handler was registered as
	 *         a general message handler.
	 */
	protected boolean registerMessageReceiver(AgentEventHandler receiver, String... prefixElements)
	{
		// TODO: if the messaging feature disappears, register with the agent; if the messaging feature appears,
		// register with that.
		if((getAgent() != null) && (getAgent().hasFeature(StandardAgentFeature.MESSAGING.toAgentFeatureDesignation())))
		{
			// the implementation somewhat non-intuitively uses the fact that the method in MessagingFeature that is
			// used has the same name.
			CompositeAgentFeature msgr = getAgent().getFeature(StandardAgentFeature.MESSAGING.toAgentFeatureDesignation());
			return msgr.registerMessageReceiver(receiver, prefixElements);
		}
		registerHandler(AgentEventType.AGENT_MESSAGE, receiver);
		return false;
	}
	
	/**
	 * Retrieve the complete path of the endpoint of the current feature, as specified by the feature by means of the
	 * path elements in arguments, and adding the agent's address. This method is meant to be used by features that send
	 * messages to obtain the 'source' part of the message.
	 * <p>
	 * The obtained result can be used by another agent to send a message (reply) to the feature / service.
	 * 
	 * @param pathElements
	 *            - the elements in the path to the feature.
	 * @return the requested complete path, as generated by the messaging feature. <code>null</code> is returned if the
	 *         feature is not available.
	 */
	protected String getFeatureEndpoint(String... pathElements)
	{
		try
		{
			return ((MessagingFeature) getAgent()
					.getFeature(StandardAgentFeature.MESSAGING.toAgentFeatureDesignation()))
							.makeLocalPath(pathElements);
		} catch(NullPointerException e)
		{
			// messaging feature not available
			return null;
		}
	}
	
	/**
	 * Method that relays the sending of a message, without the need to interact with the messaging feature directly.
	 * This version of the method converts the target agent name to an agent address and assembles it with the elements
	 * of the target internal path.
	 * 
	 * @param content
	 *            - the content of the message.
	 * @param sourceEndpoint
	 *            - the source endpoint, as a complete path. See {@link #getFeatureEndpoint(String...)}.
	 * @param targetAgent
	 *            - the name of the target agent, as a name that can be passed to
	 *            {@link MessagingFeature#getAgentAddress(String)}.
	 * @param targetPathElements
	 *            - elements in the internal path of the target.
	 * @return <code>true</code> if the message has been successfully sent.
	 */
	protected boolean sendMessage(String content, String sourceEndpoint, String targetAgent,
			String... targetPathElements)
	{
		MessagingFeature msgr = (MessagingComponent) getAgent()
				.getFeature(StandardAgentFeature.MESSAGING.toAgentFeatureDesignation());
		if(msgr != null)
			return msgr.sendMessage(msgr.makePath(targetAgent, targetPathElements), sourceEndpoint, content);
		return false;
	}
	
	/**
	 * Method that relays the sending of a message, without the need to interact with the messaging feature directly.
	 * This version of the method takes the complete target endpoint.
	 * 
	 * @param content
	 *            - the content of the message.
	 * @param sourceEndpoint
	 *            - the source endpoint, as a complete path. See {@link #getFeatureEndpoint(String...)}.
	 * @param targetEndpoint
	 *            - the destination endpoint, as a complete path. Such a path could be generated using
	 *            {@link MessagingFeature#makePath(String, String...)}.
	 * @param targetPathElements
	 *            - elements in the internal path of the target.
	 * @return <code>true</code> if the message has been successfully sent.
	 */
	protected boolean sendMessageToEndpoint(String content, String sourceEndpoint, String targetEndpoint)
	{
		MessagingFeature msgr = (MessagingFeature) getAgent().getFeature(StandardAgentFeature.MESSAGING);
		if(msgr != null)
			return msgr.sendMessage(targetEndpoint, sourceEndpoint, content);
		return false;
	}
	
	/**
	 * Facilitates the sending of a message in reply to a previous message, just by reversing the source and destination
	 * endpoints and adding the content.
	 * 
	 * @param content
	 *            - the content of the new message.
	 * @param replyTo
	 *            - the message to reply to. From this message the source and destination endpoints are taken and
	 *            reversed into the new message.
	 * @return <code>true</code> if the message has been successfully sent.
	 */
	protected boolean sendReply(String content, AgentEvent replyTo)
	{
		return sendMessageToEndpoint(content, replyTo.get(MessagingFeature.DESTINATION_PARAMETER),
				replyTo.get(MessagingFeature.SOURCE_PARAMETER));
	}
	
}
