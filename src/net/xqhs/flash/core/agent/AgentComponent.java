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
package net.xqhs.flash.core.agent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.agent.AgentEvent.AgentEventHandler;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.behavior.BehaviorComponent;
import net.xqhs.flash.core.agent.claim.ClaimComponent;
import net.xqhs.flash.core.agent.kb.CognitiveComponent;
import net.xqhs.flash.core.agent.messaging.MessagingComponent;
import net.xqhs.flash.core.agent.movement.MovementComponent;
import net.xqhs.flash.core.agent.parametric.ParametricComponent;
import net.xqhs.flash.core.agent.visualization.VisualizableComponent;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.logging.DumbLogger;
import net.xqhs.util.logging.Logger;
import tatami.amilab.AmILabComponent;
import tatami.core.util.ParameterSet;

/**
 * This class serves as base for agent components. A component is characterized by its functionality, denominated by
 * means of its name -- an instance of {@link AgentComponentName}.
 * <p>
 * A component can belong to at most one {@link CompositeAgent}, which is its parent. When created, the component has no
 * parent; a parent will be set afterwards and the component notified.
 * <p>
 * In the lifecycle of a component, it will be constructed and pre-loaded, before receiving an AGENT_START event.
 * <p>
 * The class {@link AgentComponent} itself contains various methods that may be of use for any specific component. These
 * methods are <code>protected</code>, as they should be accessible only from the inside of the component, not from the
 * outside. These methods are enumerated below:
 * <ul>
 * <li>Methods that are called when events happen. These methods should be overridden by extending classes in order to
 * intercept initialization (constructor, <code>componentInitializer()</code>, <code>preload()</code>), parent change (
 * <code>parentChangeNotifier()</code>), and other events. The class contains methods that, <i>by default</i>, are
 * registered as event handlers for some agent events (agent start/stop, simulation start/pause, before/after move).
 * Other handlers may be registered instead by the implementation, so these methods ( <code>at*()</code>) are not
 * guaranteed to be called when the event occurs. It is the choice of the developer of the extending class if events are
 * to be handled by overriding these methods or by registering new handlers. There are events for which default handlers
 * are not registered (e.g. AGENT_MESSAGE).
 * 
 * <li>Methods to access component creation data: <code>getComponentName()</code> and <code>getComponentData()</code>.
 * 
 * <li>Methods that relay to access some package-accessible functionality from {@link CompositeAgent}:
 * <code>getAgentName()</code>, <code>getParent()</code>, <code>getPlatformLink()</code> -- retrieves an {@link Object}
 * that can be casted to the specific implementation of what the platform offers in order to access platform-specific
 * functionality; <code>getAgentComponent()</code> -- retrieve an agent component by name.
 * 
 * <li>Methods for event handling: <code>postAgentEvent()</code> to post a new agent event to the agent's event queue;
 * and <code>registerHandler()</code> to register handlers for agent events. These handlers are the handlers associated
 * to <i>this</i> component.
 * 
 * <li>Functionality specific to other commonly-used components:
 * <ul>
 * 
 * <li>visualization: the method <code>getAgentLog()</code> retrieves the {@link Logger} implementation used by the
 * agent, relying the call to the {@link VisualizableComponent} of the agent (if any).
 * 
 * <li>messaging: the methods <code>registerMessageReceiver</code>, <code>sendMessage</code> and
 * <code>getComponentEndpoint</code> call methods in the {@link MessagingComponent} of the agent (if any) to register a
 * handler for messages sent to a particular internal endpoint; to send a message; or to compute the endpoint of
 * <i>this</i> component.
 * </ul>
 * </ul>
 * <p>
 * TODO: implement mechanisms to enable caching of component references, invalidating the cache only at specific agent
 * events; add the event COMPONENT_CHANGE.
 * <p>
 * 
 * @author Andrei Olaru
 */
public abstract class AgentComponent implements Serializable
{
	/**
	 * The class UID.
	 */
	private static final long serialVersionUID = -8282262747231347473L;
	
	/**
	 * Alias of {@link ParameterSet}.
	 * 
	 * @author Andrei Olaru
	 */
	public static class ComponentCreationData extends ParameterSet
	{
		/**
		 * The serial UID.
		 */
		private static final long serialVersionUID = 5069937206709568881L;
	}
	
	/**
	 * Enumeration of available component names / functionalities. These are the standard types of components. Other
	 * types of components may be added to an agent (TODO).
	 * <p>
	 * The enumeration entries also contain information about the default implementation of the specified component
	 * type. The name of the implementation class can be given when creating the entry, or can be inferred based on the
	 * name of the entry and the constants in the enumeration.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum AgentComponentName {
		/**
		 * The name of a component extending {@link ParametricComponent}.
		 */
		PARAMETRIC_COMPONENT,
		
		/**
		 * The name of a component extending {@link VisualizableComponent}.
		 */
		VISUALIZABLE_COMPONENT(
				AgentComponentName.AGENT_COMPONENT_PACKAGE_ROOT + ".visualization.VisualizableComponent"),
				
		/**
		 * The name of a component extending {@link CognitiveComponent}.
		 */
		COGNITIVE_COMPONENT(AgentComponentName.AGENT_COMPONENT_PACKAGE_ROOT + ".kb.ContextComponent"),
		
		/**
		 * The name of a component extending {@link MessagingComponent}.
		 */
		MESSAGING_COMPONENT,
		
		/**
		 * The name of a component extending {@link MovementComponent}.
		 */
		MOVEMENT_COMPONENT,
		
		/**
		 * The name of a component extending {@link BehaviorComponent}.
		 */
		BEHAVIOR_COMPONENT,
		
		/**
		 * The name of a component extending {@link WebserviceComponent}.
		 */
		WEBSERVICE_COMPONENT,
		
		/**
		 * The name of a component extending {@link HierarchicalComponent}.
		 */
		HIERARCHICAL_COMPONENT,
		
		/**
		 * The name of a component extending {@link ClaimComponent}.
		 */
		S_CLAIM_COMPONENT(AgentComponentName.AGENT_COMPONENT_PACKAGE_ROOT + ".claim.ClaimComponent"),
		
		/**
		 * The name of a component extending {@link AmILabComponent}.
		 */
		AMILAB_COMPONENT("tatami.amilab.AmILabComponent"),
		
		/**
		 * TEMPORARY type for testing. TODO: remove this type.
		 */
		TESTING_COMPONENT,
		
		;
		
		/**
		 * Suffix for component classes.
		 */
		private static final String	AGENT_COMPONENT_CLASS_SUFFIX	= "Component";
		/**
		 * Default parent package packages containing default component implementations.
		 */
		private static final String	AGENT_COMPONENT_PACKAGE_ROOT	= "tatami.core.agent";
																	
		/**
		 * The fully qualified class name of the default component implementation.
		 */
		String						componentClass;
									
		/**
		 * The name of the component, as appearing in the scenario file.
		 */
		String						componentName;
									
		/**
		 * Specifies the fully qualified class name of the component implementation.
		 * 
		 * @param classname
		 *            - the fully qualified class name.
		 */
		private AgentComponentName(String classname)
		{
			// FIXME: check that package and class exist
			componentClass = classname;
			componentName = this.name().split("_")[0].toLowerCase();
		}
		
		/**
		 * Inferres the class of the component implementation based on the name of the component and constants in this
		 * class.
		 */
		private AgentComponentName()
		{
			// FIXME: check that package and class exist
			// lower case entry name, without "_COMPONENT" suffix.
			componentName = this.name().split("_")[0].toLowerCase();
			String componentPackage = AGENT_COMPONENT_PACKAGE_ROOT + "." + componentName;
			componentClass = componentPackage + "." + componentName.substring(0, 1).toUpperCase()
					+ componentName.substring(1) + AGENT_COMPONENT_CLASS_SUFFIX;
		}
		
		/**
		 * Gets the specified or inferred class name for the default implementation of the component.
		 * 
		 * @return the class name.
		 */
		public String getClassName()
		{
			return componentClass;
		}
		
		/**
		 * Gets the name of the component, as appearing in the scenario file.
		 * 
		 * @return the name of the component.
		 */
		public String componentName()
		{
			return componentName;
		}
		
		/**
		 * Returns the {@link AgentComponentName} instance that corresponds to the specified name.
		 * <p>
		 * E.g. for the name "parametric" the return value will be the instance named PARAMETRIC_COMPONENT.
		 * 
		 * @param componentName
		 *            - the name of the component, as appearing in the scenario file.
		 * @return the corresponding {@link AgentComponentName} instance.
		 */
		public static AgentComponentName toComponentName(String componentName)
		{
			try
			{
				return AgentComponentName
						.valueOf(componentName.toUpperCase() + "_" + AGENT_COMPONENT_CLASS_SUFFIX.toUpperCase());
			} catch(Exception e)
			{
				return null;
			}
		}
	}
	
	/**
	 * The name of the component, as instance of {@link AgentComponentName}.
	 */
	private AgentComponentName						componentName;
	/**
	 * Creation data for the component, loaded in {@link #preload(ComponentCreationData, XMLNode, Logger)}. The field is
	 * initialized with an empty structure, so that it is guaranteed that it will never be <code>null</code> after
	 * construction.
	 */
	private ComponentCreationData					componentData;
	/**
	 * The {@link CompositeAgent} instance that this instance is part of.
	 */
	private CompositeAgent							parentAgent;
	/**
	 * The {@link AgentEventHandler} instances that respond to various events in the agent.
	 */
	private Map<AgentEventType, AgentEventHandler>	eventHandlers;
													
	/**
	 * The constructor assigns the name to the component.
	 * <p>
	 * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
	 * parent agent or on other components, as when the component is created, the {@link AgentComponent#parentAgent}
	 * member is <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes
	 * by calling the method {@link AgentComponent#parentChangeNotifier(CompositeAgent)}.
	 * <p>
	 * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
	 * {@link #componentInitializer()} method.
	 * 
	 * @param name
	 *            - the name of the component, as instance of {@link AgentComponentName}.
	 */
	protected AgentComponent(AgentComponentName name)
	{
		componentName = name;
		
		// dummy component data, in case no other is preloaded
		componentData = new ComponentCreationData();
		componentData.ensureLocked();
		
		// register
		eventHandlers = new HashMap<AgentEventType, AgentEventHandler>();
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
		
		componentInitializer();
	}
	
	/**
	 * Extending <b>anonymous</b> classes can override this method to perform actions when the component is created. The
	 * method is called at the end of the constructor.
	 * <p>
	 * Extending classes should always call super.componentInitializer() first.
	 * <p>
	 * IMPORTANT: The note in {@link #AgentComponent(AgentComponentName)} also applies to this method.
	 * <p>
	 * VERY IMPORTANT: initializations done in this method are done before all initializations in extending
	 * constructors.
	 */
	protected void componentInitializer()
	{
		// this class does not do anything here.
	}
	
	/**
	 * Extending classes should override this method to verify and pre-load component data, based on scenario data. The
	 * component should perform agent-dependent initialization actions when
	 * {@link #parentChangeNotifier(CompositeAgent)} is called, and actions depending on other components after the
	 * AGENT_START event has occurred.
	 * <p>
	 * If the component is surely not going to be able to load, <code>false</code> will be returned. For any non-fatal
	 * issues, the method should return <code>true</code> and output warnings in the specified log.
	 * <p>
	 * The method loads the parameters into {@link #componentData} and locks them.
	 * <p>
	 * IMPORTANT: The note in {@link #AgentComponent(AgentComponentName)} also applies to this method.
	 * <p>
	 * ALSO IMPORTANT: always call <code>super.preload()</code> first.
	 * <p>
	 * This method is normally <code>protected</code>, so it can only be called from the component itself, or from the
	 * tatami.core.agent package (through {@link AgentComponent#preload}). For testing purposes, one may override
	 * {@link #componentInitializer()} to call {@link #preload}.
	 * 
	 * @param parameters
	 *            - parameters for creating the component. The parameters will be locked (see
	 *            {@link ComponentCreationData#lock()} from this moment on.
	 * @param scenarioNode
	 *            - the {@link XMLNode} that contains the complete data for creating the component, as stated in the
	 *            scenario file.
	 * @param agentPackages
	 *            - the packages where the agent may look for files.
	 * @param log
	 *            - the {@link Logger} in which to output any potential problems (as warnings or errors).
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	protected boolean preload(ComponentCreationData parameters, XMLNode scenarioNode, List<String> agentPackages,
			Logger log)
	{
		if(parameters != null)
		{
			parameters.ensureLocked();
			componentData = parameters;
		}
		return true;
	}
	
	/**
	 * Extending classes can override this method to perform actions when the parent of the component changes, ot when
	 * the component is effectively integrated (added) in the agent.
	 * <p>
	 * The previous reference to the parent can be found in the first parameter. The current parent can be obtained by
	 * calling {@link #getParent()}.
	 * <p>
	 * Such actions may be initializations that depend on the parent or on other components of the same agent.
	 * <p>
	 * Extending classes should always call super.parentChangeNotifier() first.
	 * 
	 * @param oldParent
	 *            - the previous value for the parent, if any.
	 */
	protected void parentChangeNotifier(CompositeAgent oldParent)
	{
		// this class does not do anything here.
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
	 * Setter for the parent of the agent. If an agent instance is already a parent of this component,
	 * <code>removeParent</code> must be called first.
	 * <p>
	 * After assigning the parent, <code>the parentChangeNotifier</code> method will be called, so that extending
	 * classes can take appropriate action.
	 * 
	 * @param parent
	 *            - the {@link CompositeAgent} instance that this component is part of.
	 */
	final void setParent(CompositeAgent parent)
	{
		CompositeAgent oldParent = parentAgent;
		parentAgent = parent;
		parentChangeNotifier(oldParent);
	}
	
	/**
	 * Sets the parent of the component to <code>null</code>, effectively eliminating the component from the agent.
	 * <p>
	 * After assigning the parent, <code>the parentChangeNotifier</code> method will be called, so that extending
	 * classes can take appropriate action.
	 */
	final void removeParent()
	{
		CompositeAgent oldParent = parentAgent;
		parentAgent = null;
		parentChangeNotifier(oldParent);
	}
	
	/**
	 * The method calls the event handler of the component for the event which occurred.
	 * <p>
	 * It relays the call from the parent {@link CompositeAgent}.
	 * 
	 * @param event
	 *            - the event which occurred.
	 */
	void signalAgentEvent(AgentEvent event)
	{
		if(eventHandlers.containsKey(event.getType()))
			eventHandlers.get(event.getType()).handleEvent(event);
	}
	
	/**
	 * @return the name of the component (instance of {@link AgentComponentName}).
	 */
	protected AgentComponentName getComponentName()
	{
		return componentName;
	}
	
	/**
	 * @return the component initialization data. It cannot be modified, and it is guaranteed to not be
	 *         <code>null</code>.
	 */
	protected ComponentCreationData getComponentData()
	{
		return componentData;
	}
	
	/**
	 * Retrieves the parent of the component.
	 * 
	 * @return the {@link CompositeAgent} instance this component belongs to.
	 */
	protected CompositeAgent getParent()
	{
		return parentAgent;
	}
	
	/**
	 * Relay for calls to the method {@link CompositeAgent#getAgentName()}.
	 * 
	 * @return the name of the agent, or <code>null</code> if the component has no parent.
	 */
	protected String getAgentName()
	{
		return (parentAgent != null) ? parentAgent.getAgentName() : null;
	}
	
	/**
	 * Relay for calls to the method {@link CompositeAgent#getPlatformLink()}.
	 * 
	 * @return the platform link.
	 */
	protected Object getPlatformLink()
	{
		return parentAgent.getPlatformLink();
	}
	
	/**
	 * Relay for calls to the method {@link CompositeAgent#getComponent(AgentComponentName)}.
	 * 
	 * @param name
	 *            - the name of the component.
	 * @return the {@link AgentComponent} instance, if any. <code>null</code> otherwise.
	 */
	protected AgentComponent getAgentComponent(AgentComponentName name)
	{
		return (parentAgent != null) ? parentAgent.getComponent(name) : null;
	}
	
	/**
	 * Extending classes should use this method to register {@link AgentEventHandler} instances that would be invoked
	 * when the specified {@link AgentEventType} appears.
	 * <p>
	 * Important note: The registered handler is the handler specific to <b>this</b> component (therefore it is
	 * sufficient to have only one). Each component of the agent may contain (at most) a handler for the same event.
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
	 * Relay for calls to the method in {@link CompositeAgent}.
	 * 
	 * @param event
	 *            - the event to disseminate.
	 */
	protected void postAgentEvent(AgentEvent event)
	{
		if(parentAgent != null)
			parentAgent.postAgentEvent(event);
	}
	
	/**
	 * @return the log of the agent, or an instance of {@link DumbLogger}, if one is not present or cannot be obtained.
	 *         <code>null</code> should never be returned.
	 */
	protected Logger getAgentLog()
	{
		try
		{
			return ((VisualizableComponent) getAgentComponent(AgentComponentName.VISUALIZABLE_COMPONENT)).getLog();
		} catch(NullPointerException e)
		{
			return DumbLogger.get();
		}
	}
	
	/**
	 * Handles the registration of an event handler for messages to a target (inside the agent) with the specified
	 * prefix (elements of the internal path of the endpoint).
	 * <p>
	 * If the component has a parent and a {@link MessagingComponent} exists, the handler will be registered with the
	 * messaging component. Otherwise, the handler be registered to receive any messages, regardless of prefix. In the
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
		// TODO: if the messaging component disappears, register with the agent; if the messaging component appears,
		// register with that.
		if((parentAgent != null) && (parentAgent.hasComponent(AgentComponentName.MESSAGING_COMPONENT)))
		{
			// the implementation somewhat non-intuitively uses the fact that the method in MessagingComponent that is
			// used has the same name.
			AgentComponent msgr = parentAgent.getComponent(AgentComponentName.MESSAGING_COMPONENT);
			return msgr.registerMessageReceiver(receiver, prefixElements);
		}
		registerHandler(AgentEventType.AGENT_MESSAGE, receiver);
		return false;
	}
	
	/**
	 * Retrieve the complete path of the endpoint of the current component, as specified by the component by means of
	 * the path elements in arguments, and adding the agent's address. This method is meant to be used by components
	 * that send messages to obtain the 'source' part of the message.
	 * <p>
	 * The obtained result can be used by another agent to send a message (reply) to the component / service.
	 * 
	 * @param pathElements
	 *            - the elements in the path to the component.
	 * @return the requested complete path, as generated by the messaging component. <code>null</code> is returned if
	 *         the component is not available.
	 */
	protected String getComponentEndpoint(String... pathElements)
	{
		try
		{
			return ((MessagingComponent) parentAgent.getComponent(AgentComponentName.MESSAGING_COMPONENT))
					.makeLocalPath(pathElements);
		} catch(NullPointerException e)
		{
			// messaging component not available
			return null;
		}
	}
	
	/**
	 * Method that relays the sending of a message, without the need to interact with the messaging component directly.
	 * This version of the method converts the target agent name to an agent address and assembles it with the elements
	 * of the target internal path.
	 * 
	 * @param content
	 *            - the content of the message.
	 * @param sourceEndpoint
	 *            - the source endpoint, as a complete path. See {@link #getComponentEndpoint(String...)}.
	 * @param targetAgent
	 *            - the name of the target agent, as a name that can be passed to
	 *            {@link MessagingComponent#getAgentAddress(String)}.
	 * @param targetPathElements
	 *            - elements in the internal path of the target.
	 * @return <code>true</code> if the message has been successfully sent.
	 */
	protected boolean sendMessage(String content, String sourceEndpoint, String targetAgent,
			String... targetPathElements)
	{
		MessagingComponent msgr = (MessagingComponent) parentAgent.getComponent(AgentComponentName.MESSAGING_COMPONENT);
		if(msgr != null)
			return msgr.sendMessage(msgr.makePath(targetAgent, targetPathElements), sourceEndpoint, content);
		return false;
	}
	
	/**
	 * Method that relays the sending of a message, without the need to interact with the messaging component directly.
	 * This version of the method takes the complete target endpoint.
	 * 
	 * @param content
	 *            - the content of the message.
	 * @param sourceEndpoint
	 *            - the source endpoint, as a complete path. See {@link #getComponentEndpoint(String...)}.
	 * @param targetEndpoint
	 *            - the destination endpoint, as a complete path. Such a path could be generated using
	 *            {@link MessagingComponent#makePath(String, String...)}.
	 * @param targetPathElements
	 *            - elements in the internal path of the target.
	 * @return <code>true</code> if the message has been successfully sent.
	 */
	protected boolean sendMessageToEndpoint(String content, String sourceEndpoint, String targetEndpoint)
	{
		MessagingComponent msgr = (MessagingComponent) parentAgent.getComponent(AgentComponentName.MESSAGING_COMPONENT);
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
		return sendMessageToEndpoint(content, replyTo.get(MessagingComponent.DESTINATION_PARAMETER),
				replyTo.get(MessagingComponent.SOURCE_PARAMETER));
	}
}
