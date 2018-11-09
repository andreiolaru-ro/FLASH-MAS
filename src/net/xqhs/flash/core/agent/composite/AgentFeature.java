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
import java.util.List;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation.StandardAgentFeature;
import net.xqhs.flash.core.util.ParameterSet;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.logging.Logger;

/**
 * This class serves as base for agent feature implementation. A feature (also called a component) is characterized by
 * its functionality, identified by means of its designation -- an instance of {@link AgentFeatureDesignation}.
 * <p>
 * A feature can belong to at most one {@link CompositeAgent}, which is its parent (and, in {@link Entity} terms, its
 * context). When created, the feature has no parent; a parent will be set afterwards and the feature notified.
 * <p>
 * In the life-cycle of a feature, it will be constructed and pre-loaded, before receiving an AGENT_START event.
 * <p>
 * 
 * @author Andrei Olaru
 */
public abstract class AgentFeature implements Entity<Agent>, Serializable
{
	/**
	 * The class UID.
	 */
	private static final long						serialVersionUID	= -8282262747231347473L;
	
	/**
	 * The designation of the feature, as instance of {@link StandardAgentFeature}.
	 */
	private AgentFeatureDesignation					featureDesignation;
	/**
	 * Creation data for the feature. The field is initialized with an empty structure, so that it is guaranteed that it
	 * will never be <code>null</code> after construction.
	 */
	private ParameterSet							creationData;
	/**
	 * The {@link CompositeAgent} instance that this instance is part of.
	 */
	private CompositeAgent							parentAgent;
	
	/**
	 * The constructor assigns the designation to the feature.
	 * <p>
	 * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
	 * parent agent or on other features, as when the feature is created, the {@link AgentFeature#parentAgent} member is
	 * <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes by calling
	 * the method {@link AgentFeature#parentChangeNotifier(CompositeAgent)}.
	 * <p>
	 * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
	 * {@link #featureInitializer()} method.
	 * 
	 * @param designation
	 *            - the designation of the feature, as instance of {@link StandardAgentFeature}.
	 */
	protected AgentFeature(AgentFeatureDesignation designation)
	{
		featureDesignation = designation;
		
		// dummy feature data, in case no other is preloaded
		creationData = new ParameterSet();
		creationData.ensureLocked();
		
		featureInitializer();
	}
	
	/**
	 * Extending <b>anonymous</b> classes can override this method to perform actions when the feature is created. The
	 * method is called at the end of the constructor.
	 * <p>
	 * Extending classes should always call <code>super.featureInitializer()</code> first thing in their implementation
	 * of <code>featureInitializer()</code>.
	 * <p>
	 * IMPORTANT: The note regarding initializations in {@link #AgentFeature(AgentFeatureDesignation)} also applies to
	 * this method.
	 * <p>
	 * VERY IMPORTANT: initializations done in this method are done before all initializations in extending
	 * constructors.
	 */
	protected void featureInitializer()
	{
		// this class does not do anything here.
	}
	
	/**
	 * Extending classes should override this method to verify and pre-load feature data, based on deployment data. The
	 * feature should perform agent-dependent initialization actions when {@link #parentChangeNotifier(CompositeAgent)}
	 * is called, and actions depending on other features after the AGENT_START event has occurred.
	 * <p>
	 * If the feature is surely not going to be able to load, <code>false</code> will be returned. For any non-fatal
	 * issues, the method should return <code>true</code> and output warnings in the specified log.
	 * <p>
	 * The method loads the parameters into {@link #creationData} and locks them.
	 * <p>
	 * IMPORTANT: The note regarding initializations in {@link #AgentFeature(AgentFeatureDesignation)} also applies to
	 * this method.
	 * <p>
	 * ALSO IMPORTANT: always call <code>super.preload()</code> first.
	 * <p>
	 * This method is normally <code>protected</code>, so it can only be called from the feature itself, or from the
	 * same package (through {@link AgentFeature#preload}). For testing purposes, one may override
	 * {@link #featureInitializer()} to call {@link #preload}.
	 * 
	 * @param parameters
	 *            - parameters for creating the feature. The parameters will be locked (see {@link ParameterSet#lock()}
	 *            from this moment on.
	 * @param scenarioNode
	 *            - the {@link XMLNode} that contains the complete data for creating the feature, as stated in the
	 *            scenario file.
	 * @param agentPackages
	 *            - the packages where the agent may look for files.
	 * @param log
	 *            - the {@link Logger} in which to output any potential problems (as warnings or errors).
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	protected boolean preload(ParameterSet parameters, XMLNode scenarioNode, List<String> agentPackages, Logger log)
	{
		if(parameters != null)
		{
			parameters.ensureLocked();
			creationData = parameters;
		}
		return true;
	}
	
	/**
	 * Extending classes can override this method to perform actions when the parent of the feature changes, ot when the
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
	 * Setter for the parent agent. If an agent instance is already a parent of this feature, <code>removeParent</code>
	 * must be called first.
	 * <p>
	 * After assigning the parent, <code>the parentChangeNotifier</code> method will be called, so that extending
	 * classes can take appropriate action.
	 * 
	 * @param parent
	 *            - the {@link CompositeAgent} instance that this feature is part of.
	 */
	final void setAgent(CompositeAgent parent)
	{
		CompositeAgent oldParent = parentAgent;
		parentAgent = parent;
		parentChangeNotifier(oldParent);
	}
	
	/**
	 * Sets the parent of the feature to <code>null</code>, effectively eliminating the feature from the agent.
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
	 * The method calls is called by the parent {@link CompositeAgent} when an event occurs.
	 * 
	 * @param event
	 *            - the event which occurred.
	 */
	protected void signalAgentEvent(AgentEvent event)
	{
		// This method does nothing here.
	}
	
	/**
	 * @return the designation of the feature (instance of {@link StandardAgentFeature}).
	 */
	protected AgentFeatureDesignation getFeatureDesignation()
	{
		return featureDesignation;
	}
	
	/**
	 * @return the feature initialization data. It cannot be modified, and it is guaranteed to not be <code>null</code>.
	 */
	protected ParameterSet getFeatureData()
	{
		return creationData;
	}
	
	/**
	 * Retrieves the parent of the feature.
	 * 
	 * @return the {@link CompositeAgent} instance this feature belongs to.
	 */
	protected CompositeAgent getAgent()
	{
		return parentAgent;
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
	
}
