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

import java.util.Iterator;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.AgentFeature.AgentFeatureType;
import net.xqhs.flash.core.agent.AgentFeature.ComponentCreationData;
import net.xqhs.flash.core.agent.parametric.AgentParameters;
import net.xqhs.flash.core.agent.parametric.ParametricComponent;
import net.xqhs.flash.core.node.AgentCreationData;
import net.xqhs.flash.core.node.AgentLoader;
import net.xqhs.flash.core.node.AgentLoader.StandardAgentLoaderType;
import net.xqhs.flash.core.support.Support;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.logging.Logger;
import tatami.simulation.AgentManager;

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
	 * specified, it the component is standard and its class is specified by the corresponding {@link AgentFeatureType}
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
	
	public CompositeAgentLoader(TreeParameterSet config)
	{
		// nothing to do for the moment.
	}
	
	@Override
	public boolean preload(AgentCreationData agentCreationData, Support platformLoader, Logger log)
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
				AgentFeatureType component = AgentFeatureType.toComponentName(componentName);
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
			if(AgentFeatureType.PARAMETRIC_COMPONENT.componentName().equals(componentName))
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
	
	@Override
	public AgentManager load(AgentCreationData agentCreationData)
	{
		CompositeAgent agent = new CompositeAgent();
		for(Object componentObj : agentCreationData.getParameters().getObjects(COMPONENT_PARAMETER_NAME))
			agent.addComponent((AgentFeature) componentObj);
		return agent;
	}
}
