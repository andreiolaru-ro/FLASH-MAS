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

import java.util.Iterator;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.composite.AgentFeature.ComponentCreationData;
import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation.StandardAgentFeature;
import net.xqhs.flash.core.agent.parametric.ParametricComponent;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.logging.Logger;

/**
 * Agent loader for agents extending {@link CompositeAgent}.
 * 
 * @author Andrei Olaru
 */
public class CompositeAgentLoader implements Loader<Agent>
{
	/**
	 * Name of XML nodes in the scenario representing components.
	 */
	private static final String	FEATURE_NODE_NAME		= "feature";
	/**
	 * The name of the attribute representing the name of the component in the component node.
	 */
	private static final String	FEATURE_NAME_ATTRIBUTE	= "name";
	/**
	 * The name of the attribute representing the class of the component in the component node. The class may not be
	 * specified, it the component is standard and its class is specified by the corresponding {@link StandardAgentFeature}
	 * entry.
	 */
	private static final String	FEATURE_CLASS_ATTRIBUTE	= "classpath";
	/**
	 * The name of nodes containing component parameters.
	 */
	private static final String	PARAMETER_NODE_NAME		= "parameter";
	/**
	 * The name of the attribute of a parameter node holding the name of the parameter.
	 */
	private static final String	PARAMETER_NAME			= DeploymentConfiguration.PARAMETER_NAME;
	/**
	 * The name of the attribute of a parameter node holding the value of the parameter.
	 */
	private static final String	PARAMETER_VALUE			= DeploymentConfiguration.PARAMETER_VALUE;
	
	/**
	 * The constructor receives the configuration loaded at boot.
	 * 
	 * @param config
	 *            - the configuration.
	 * @param log
	 *            - the {@link Logger} in which to output any potential problems.
	 */
	public CompositeAgentLoader(TreeParameterSet config, Logger log)
	{
		// nothing to do for the moment.
	}
	
	/**
	 * The method checks potential problems that could appear in the creation of an agent, as specified by the
	 * information in the argument. Potential problems relate, for example, to inexistent classes.
	 * <p>
	 * The method also adds information to the agent creation data, in order to speed up the loading process.
	 * <p>
	 * If the agent will surely not be able to load, <code>false</code> will be returned. For any non-fatal issues, the
	 * method should return <code>true</code> and output warnings in the log.
	 * 
	 * @param agentCreationData
	 *            - the {@link TreeParameterSet}, as loaded from the deployment file.
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	@Override
	public boolean preload(TreeParameterSet agentCreationData)
	{
		String logPre = agentCreationData.getName() + ":";
		Iterator<XMLNode> componentIt = agentCreationData.getNode().getNodeIterator(FEATURE_NODE_NAME);
		while(componentIt.hasNext())
		{
			XMLNode componentNode = componentIt.next();
			String componentName = componentNode.getAttributeValue(FEATURE_NAME_ATTRIBUTE);
			
			// get component class
			String componentClass = componentNode.getAttributeValue(FEATURE_CLASS_ATTRIBUTE);
			if(componentClass == null)
			{
				StandardAgentFeature component = StandardAgentFeature.toStandardAgentFeature(componentName);
				if(component != null)
				{
					if(platformLoader != null)
					{
						String recommendedClass = platformLoader.getRecommendedFeatureImplementation(component);
						if(recommendedClass != null)
							componentClass = recommendedClass;
					}
					if(componentClass == null)
						componentClass = component.getClassName();
				}
				else
				{
					log.error(logPre + "Component [" + componentName
							+ "] unknown and component class not specified. Component will not be available.");
					continue;
				}
			}
			if(componentClass == null)
			{
				log.error(logPre + "Component class not specified for component [" + componentName
						+ "]. Component will not be available.");
				continue;
			}
			
			if(PlatformUtils.classExists(componentClass))
				log.trace(logPre + "component [" + componentName + "] can be loaded");
			else
			{
				log.error(logPre + "Component class [" + componentName + " | " + componentClass
						+ "] not found; it will not be loaded.");
				continue;
			}
			
			AgentFeature component = null;
			try
			{
				component = (AgentFeature) PlatformUtils.loadClassInstance(this, componentClass, new Object[0]);
				log.trace("component [] created for agent []. pre-loading...", componentClass,
						agentCreationData.getName());
			} catch(Exception e)
			{
				log.error("Component [] failed to load; it will not be available for agent []:", componentClass,
						agentCreationData.getName(), PlatformUtils.printException(e));
				continue;
			}
			
			// load component arguments
			ComponentCreationData componentData = new ComponentCreationData();
			Iterator<XMLNode> paramsIt = componentNode.getNodeIterator(PARAMETER_NODE_NAME);
			while(paramsIt.hasNext())
			{
				XMLNode param = paramsIt.next();
				componentData.add(param.getAttributeValue(PARAMETER_NAME), param.getAttributeValue(PARAMETER_VALUE));
			}
			if(StandardAgentFeature.PARAMETRIC_COMPONENT.featureName().equals(componentName))
				componentData.addObject(ParametricComponent.COMPONENT_PARAMETER_NAME,
						agentCreationData.getParameters());
			
			if(component.preload(componentData, componentNode, agentCreationData.getPackages(), log))
			{
				agentCreationData.getParameters().addObject(COMPONENT_PARAMETER_NAME, component);
				log.trace("component [] pre-loaded for agent []", componentClass, agentCreationData.getName());
			}
			else
				log.error("Component [] failed pre-loading step; it will not be available for agent [].",
						componentClass, agentCreationData.getName());
		}
		
		return true;
	}
	
	/**
	 * The method loads all the information necessary for the creation of an agent and returns an {@link Agent} instance
	 * used to manage the life-cycle of the loaded agent.
	 * 
	 * @param agentCreationData
	 *            - the {@link TreeParameterSet}, as loaded at deployment.
	 * @return an {@link Agent} instance for the loaded agent.
	 */
	@Override
	public Agent load(TreeParameterSet agentCreationData)
	{
		CompositeAgent agent = new CompositeAgent();
		for(Object componentObj : agentCreationData.getParameters().getObjects(COMPONENT_PARAMETER_NAME))
			agent.addFeature((AgentFeature) componentObj);
		return agent;
	}
}
