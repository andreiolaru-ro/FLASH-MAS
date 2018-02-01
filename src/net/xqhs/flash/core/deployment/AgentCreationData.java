/*******************************************************************************
 * Copyright (C) 2013 Andrei Olaru, Marius-Tudor Benea, Nguyen Thi Thuy Nga, Amal El Fallah Seghrouchni, Cedric Herpson.
 * 
 * This file is part of tATAmI-PC.
 * 
 * tATAmI-PC is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * tATAmI-PC is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with tATAmI-PC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.deployment;

import java.util.ArrayList;
import java.util.List;

import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.XML.XMLTree.XMLNode;

/**
 * Class containing the data for the creation of an agent. This information is all the information needed by (and
 * provided to) {@link SimulationManager} to create an agent.
 * <p>
 * The data specified in the deployment is available in the {@link TreeParameterSet} instance, as it is loaded by the
 * {@link BootSettingsManager}. Some data (agentName and platform) is covered by the enclosed {@link TreeParameterSet}
 * instance, but it is also added separately so as to guarantee its availability.
 * 
 * @author Andrei Olaru
 * 
 */
public class AgentCreationData
{
	/**
	 * The name of the agent. It cannot be <code>null</code>.
	 */
	String				agentName;
	/**
	 * The parameters to pass to the agent. The reference cannot be <code>null</code>.
	 */
	TreeParameterSet	parameters;
	/**
	 * The set of packages where the loader and the components may look for data.
	 */
	List<String>		packages;
	
	/**
	 * The name of the platform on which the agent should be loaded. It cannot be <code>null</code>.
	 */
	String				platform				= null;
	/**
	 * The {@link AgentLoader} instance that will be used to load the agent using this {@link AgentCreationData}
	 * instance. It cannot be <code>null</code>.
	 */
	AgentLoader			loader					= null;
	
	
	/**
	 * Creates a new instance of information for the creation of an agent.
	 * 
	 * @param name
	 *            - the name of the agent.
	 * @param agentParameters
	 *            - the {@link AgentParameters} instance to pass to the agent.
	 * @param agentPackages
	 *            - the set of packages where the loader and the components may look for data.
	 * @param destination
	 *            - the container on which the agent should be created.
	 * @param remote
	 *            - <code>true</code> if the container is remote (not on the local machine); <code>false</code>
	 *            otherwise.
	 * @param agentPlatform
	 *            - the platform on which the agent will execute.
	 * @param agentLoader
	 *            - the {@link AgentLoader} instance to use for loading the agent.
	 * @param scenarioNode
	 *            - the {@link XMLNode} instance corresponding to the agent, as resulted from parsing the scenario file.
	 */
	public AgentCreationData(String name, AgentParameters agentParameters, List<String> agentPackages,
			String destination, boolean remote, String agentPlatform, AgentLoader agentLoader, XMLNode scenarioNode)
	{
		if(name == null)
			throw new NullPointerException("Agent name cannot be null");
		if(agentParameters == null)
			throw new NullPointerException("Agent parameters cannot be null");
		if(agentPlatform == null)
			throw new NullPointerException("Agent platform cannot be null");
		if(agentLoader == null)
			throw new NullPointerException("Agent loader cannot be null");
		if(scenarioNode == null)
			throw new NullPointerException("XML node cannot be null");
		
		agentName = name;
		parameters = agentParameters;
		packages = (agentPackages != null) ? agentPackages : new ArrayList<String>();
		destinationContainer = destination;
		isRemote = remote;
		platform = agentPlatform;
		loader = agentLoader;
		node = scenarioNode;
	}
	
	/**
	 * @return the name of the agent.
	 */
	public String getAgentName()
	{
		return agentName;
	}
	
	/**
	 * @return the parameters to pass to the agent.
	 */
	public AgentParameters getParameters()
	{
		return parameters;
	}
	
	/**
	 * @return the list packages where the agent may look for files.
	 */
	public List<String> getPackages()
	{
		return packages;
	}
	
	/**
	 * @return <code>true</code> if the agent should be created on the local machine, <code>false</code> if it should be
	 *         created on a different machine.
	 */
	public boolean isRemote()
	{
		return isRemote;
	}
	
	/**
	 * @return the name of the container in which the agent should be created.
	 */
	public String getDestinationContainer()
	{
		return destinationContainer;
	}
	
	/**
	 * @return the name of the platform that this agent will execute on.
	 */
	public String getPlatform()
	{
		return platform;
	}
	
	/**
	 * @return the {@link AgentLoader} instance to be used for loading this agent.
	 */
	public AgentLoader getAgentLoader()
	{
		return loader;
	}
	
	/**
	 * @return a reference to the actual node resulted from parsing the scenario file.
	 */
	public XMLNode getNode()
	{
		return node;
	}
}
