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
package net.xqhs.flash.core.agent.composite;

import java.io.Serializable;

import net.xqhs.flash.core.ConfigurableEntity;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentFeature;
import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation.StandardAgentFeature;
import net.xqhs.flash.core.agent.composite.CompositeAgent.AgentState;
import net.xqhs.flash.core.util.ParameterSet;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.logging.Unit;

/**
 * This class serves as base for the implementation of agent features integrated into {@link CompositeAgent} instances.
 * <p>
 * A feature can belong to at most one {@link CompositeAgent}, which is its parent (and, in {@link Entity} terms, its
 * context). When created, the feature has no parent; a parent will be set afterwards and the feature notified.
 * <p>
 * In the life-cycle of a feature, it will be constructed and configured, then started and eventually stopped.
 * <p>
 * This implementation manages the configuration of the feature and the feature's state. It also extends {@link Unit},
 * so extending classes can easily post logging messages.
 * <p>
 * This implementation exclusively manages the relation with the feature's parent, and as such:
 * <ul>
 * <li>{@link #addContext} and {@link #removeContext} methods are final; extending classes can override
 * {@link #parentChangeNotifier};
 * <li>extending classes can access the parent agent by means of {@link #getAgent};
 * </ul>
 * <p>
 * The feature can only be {@link #start}ed and {@link #stop}ed during the corresponding parent agent states,
 * {@link AgentState#STARTING} and {@link AgentState#STOPPING}, respectively.
 * 
 * 
 * @author Andrei Olaru
 */
public class CompositeAgentFeature extends Unit implements AgentFeature, ConfigurableEntity<Agent>, Serializable
{
	/**
	 * The class UID.
	 */
	private static final long		serialVersionUID	= -8282262747231347473L;
	
	/**
	 * The designation of the feature, as instance of {@link StandardAgentFeature}.
	 */
	private AgentFeatureDesignation	featureDesignation;
	/**
	 * Creation data for the feature. The field is initialized with an empty structure, so that it is guaranteed that it
	 * will never be <code>null</code> after construction.
	 */
	private TreeParameterSet		featureConfiguration;
	/**
	 * The {@link CompositeAgent} instance that this instance is part of.
	 */
	private CompositeAgent			parentAgent;
	/**
	 * Indicates the state of the feature.
	 */
	private boolean					isRunning;
	
	/**
	 * The constructor assigns the designation to the feature.
	 * <p>
	 * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
	 * parent agent or on other features, as when the feature is created, the {@link CompositeAgentFeature#parentAgent}
	 * member is <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes
	 * by calling the method {@link CompositeAgentFeature#parentChangeNotifier(CompositeAgent)}.
	 * <p>
	 * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
	 * {@link #featureInitializer()} method.
	 * 
	 * @param designation
	 *            - the designation of the feature, as instance of {@link StandardAgentFeature}.
	 */
	protected CompositeAgentFeature(AgentFeatureDesignation designation)
	{
		featureDesignation = designation;
		
		// dummy feature data, in case no other is configured
		featureConfiguration = new TreeParameterSet();
		featureConfiguration.ensureLocked();
		
		featureInitializer();
	}
	
	/**
	 * Extending <b>anonymous</b> classes can override this method to perform actions when the feature is created. The
	 * method is called at the end of the constructor.
	 * <p>
	 * Extending classes should always call <code>super.featureInitializer()</code> first thing in their implementation
	 * of <code>featureInitializer()</code>.
	 * <p>
	 * IMPORTANT: The note in {@link #CompositeAgentFeature(AgentFeatureDesignation)} regarding initializations also
	 * applies to this method.
	 * <p>
	 * VERY IMPORTANT: initializations done in this method are done before all initializations in extending
	 * constructors.
	 */
	protected void featureInitializer()
	{
		// this class does not do anything here.
	}
	
	/**
	 * TODO
	 * 
	 * Extending classes should override this method to verify and pre-load feature data, based on deployment data. The
	 * feature should perform agent-dependent initialization actions when {@link #parentChangeNotifier(CompositeAgent)}
	 * is called, and actions depending on other features after the AGENT_START event has occurred.
	 * <p>
	 * If the feature is surely not going to be able to load, <code>false</code> will be returned. For any non-fatal
	 * issues, the method should return <code>true</code> and output warnings in the specified log.
	 * <p>
	 * The method loads the parameters into {@link #featureConfiguration} and locks them.
	 * <p>
	 * IMPORTANT: The note in {@link #CompositeAgentFeature(AgentFeatureDesignation)} regarding initializations also
	 * applies to this method.
	 * <p>
	 * ALSO IMPORTANT: overriding methods should always call <code>super.configure()</code> first.
	 * <p>
	 * 
	 * @param configuration
	 *            - parameters for creating the feature. The parameters will be locked (see {@link ParameterSet#lock()}
	 *            from this moment on.
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	@Override
	public boolean configure(TreeParameterSet configuration)
	{
		if(configuration != null)
		{
			configuration.ensureLocked();
			featureConfiguration = configuration;
		}
		return true;
	}
	
	/**
	 * Overriding methods should always call <code>super.start()</code> first.
	 */
	@Override
	public boolean start()
	{
		if(isRunning)
			return ler(false, "Feature is already running");
		if(getAgent() == null)
			return ler(false, "Features cannot start without being in context of an agent.");
		if(!getAgent().isStarting())
			return ler(false, "Features can only be started when the parent agent is starting.");
		isRunning = true;
		return true;
	}
	
	/**
	 * Overriding methods should always call <code>super.stop()</code> first.
	 */
	@Override
	public boolean stop()
	{
		if(!isRunning)
			return ler(false, "Feature is not running");
		if(getAgent() == null)
			throw new IllegalStateException(
					"Feature is " + getFeatureDesignation() + " not in the context of an agent.");
		if(!getAgent().isStopping())
			return ler(false, "Features can only be stopped when the parent agent is stopping.");
		isRunning = false;
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		return isRunning;
	}
	
	/**
	 * Extending classes can override this method to perform actions when the parent of the feature changes, or when the
	 * feature is effectively integrated (added) in the agent.
	 * <p>
	 * The previous reference to the parent can be found in the first parameter. The current parent can be obtained by
	 * calling {@link #getAgent()}.
	 * <p>
	 * Such actions may be initializations that depend on the parent or on other features of the same agent.
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
	 * @return the feature initialization data. It cannot be modified, and it is guaranteed to not be <code>null</code>.
	 */
	protected ParameterSet getFeatureData()
	{
		return featureConfiguration;
	}
	
	/**
	 * The method calls is called by the parent {@link CompositeAgent} when an event occurs.
	 * 
	 * @param event
	 *            - the event which occurred.
	 */
	protected void signalAgentEvent(AgentEvent event)
	{
		// This method does nothing here.
	}
	
	@Override
	final public AgentFeatureDesignation getFeatureDesignation()
	{
		return featureDesignation;
	}
	
	/**
	 * The return value is based on the feature's designation.
	 */
	@Override
	public String getName()
	{
		return featureDesignation.toString();
	}
	
	/**
	 * Setter for the parent agent. If an agent instance is already a parent of this feature, <code>removeParent</code>
	 * must be called first.
	 * <p>
	 * After assigning the parent, <code>the parentChangeNotifier</code> method will be called, so that extending
	 * classes can take appropriate action.
	 * 
	 * @param parent
	 *            - the {@link CompositeAgent} instance that this feature is part of.
	 */
	@Override
	public final boolean addContext(Agent parent)
	{
		if(parentAgent != null)
			return ler(false, "Parent already set");
		if(parent == null || !(parent instanceof CompositeAgent))
			return ler(false, "Parent should be a CompositeAgent instance");
		parentAgent = (CompositeAgent) parent;
		parentChangeNotifier(null);
		return true;
	}
	
	/**
	 * Sets the parent of the feature to <code>null</code>, effectively eliminating the feature from the agent.
	 * <p>
	 * After assigning the parent, <code>the parentChangeNotifier</code> method will be called, so that extending
	 * classes can take appropriate action.
	 */
	@Override
	public final boolean removeContext(Agent parent)
	{
		if(parentAgent == null)
			return ler(false, "Parent is not set");
		if(parentAgent != parent)
			return ler(false, "Argument is not the same as actual parent.");
		parentAgent = null;
		parentChangeNotifier((CompositeAgent) parent);
		return true;
	}
	
	/**
	 * Retrieves the parent of the feature.
	 * 
	 * @return the {@link CompositeAgent} that is the parent of this feature; <code>null</code> if there is no parent
	 *         set.
	 */
	final protected CompositeAgent getAgent()
	{
		return parentAgent;
	}
}
