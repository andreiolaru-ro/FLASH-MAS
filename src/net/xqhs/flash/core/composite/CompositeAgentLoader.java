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
package net.xqhs.flash.core.composite;

import java.util.Iterator;
import java.util.List;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.logging.Logger;

/**
 * Agent loader for agents extending {@link CompositeAgent}.
 * <p>
 * The choice of using a specialized loader as opposed to using the default loader and doing all the loading inside the
 * composite agent was made so as to decouple dynamic class loading, as well as calls to
 * {@link ClassFactory#loadClassInstance} from the actual implementation of {@link CompositeAgent}.
 * 
 * @author Andrei Olaru
 */
public class CompositeAgentLoader implements Loader<Agent>
{
	/**
	 * The name of attrbutes containing entity names.
	 */
	protected static final String	NAME_ATTRIBUTE_NAME		= DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
	/**
	 * Name of XML nodes in the scenario representing components.
	 */
	private static final String		SHARD_NODE_NAME			= "shard";
	/**
	 * The name of the attribute representing the class of the component in the component node. The class may not be
	 * specified, it the component is standard and its class is specified by the corresponding
	 * {@link StandardAgentShard} entry.
	 */
	private static final String		SHARD_CLASS_PARAMETER	= SimpleLoader.CLASSPATH_KEY;
	
	/**
	 * Logger to use during the loading process.
	 */
	Logger							log;
	
	@Override
	public boolean configure(MultiTreeMap config, Logger _log)
	{
		log = _log;
		return true;
	}
	
	@Override
	public boolean preload(MultiTreeMap configuration)
	{
		return preload(configuration, null);
	}
	
	/**
	 * The method checks potential problems that could appear in the creation of an agent, as specified by the
	 * information in the argument. Potential problems relate, for example, to inexistent classes for features.
	 * <p>
	 * The method creates the necessary {@link AgentShard} instances and pre-loads them.
	 * <p>
	 * If the agent will surely not be able to load, <code>false</code> will be returned. For any non-fatal issues, the
	 * method should return <code>true</code> and output warnings in the log.
	 * 
	 * @param agentCreationData
	 *            - the {@link MultiTreeMap}, as loaded from the deployment file.
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	@Override
	public boolean preload(MultiTreeMap agentCreationData, List<Entity<?>> context)
	{
		String logPre = (agentCreationData.isSimple(NAME_ATTRIBUTE_NAME) ? agentCreationData.get(NAME_ATTRIBUTE_NAME)
				: "<agent>") + ":";
		for(MultiTreeMap shardConfig : agentCreationData.getTrees(SHARD_NODE_NAME))
		{
			String shardName = shardConfig.get(NAME_ATTRIBUTE_NAME);
			
			// get feature class
			String shardClass = shardConfig.get(SHARD_CLASS_PARAMETER);
			if(shardClass == null)
			{
				if(shardName == null)
				{
					log.error(logPre + "Shard has neither name nor class specified. Shard will not be available.");
					continue;
				}
				AgentShardDesignation shardDesignation = AgentShardDesignation.autoFeature(shardName);
				if(platformLoader != null)
				{
					String recommendedClass = platformLoader.getRecommendedShardImplementation(shardDesignation);
					if(recommendedClass != null)
						shardClass = recommendedClass;
				}
				if(shardClass == null)
					shardClass = shardDesignation.getClassName();
			}
			if(shardClass == null)
			{
				log.error(logPre + "Component class not specified for component [" + shardName
						+ "]. Component will not be available.");
				continue;
			}
			
			if(PlatformUtils.classExists(shardClass))
				log.trace(logPre + "component [" + shardName + "] can be loaded");
			else
			{
				log.error(logPre + "Component class [" + shardName + " | " + shardClass
						+ "] not found; it will not be loaded.");
				continue;
			}
			
			AgentShardCore component = null;
			try
			{
				component = (AgentShardCore) PlatformUtils.loadClassInstance(this, shardClass, new Object[0]);
				log.trace("component [] created for agent []. pre-loading...", shardClass, agentCreationData.getName());
			} catch(Exception e)
			{
				log.error("Component [] failed to load; it will not be available for agent []:", shardClass,
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
			if(StandardAgentShard.PARAMETRIC_COMPONENT.featureName().equals(shardName))
				componentData.addObject(ParametricComponent.COMPONENT_PARAMETER_NAME,
						agentCreationData.getParameters());
			
			if(component.preload(componentData, componentNode, agentCreationData.getPackages(), log))
			{
				agentCreationData.getParameters().addObject(COMPONENT_PARAMETER_NAME, component);
				log.trace("component [] pre-loaded for agent []", shardClass, agentCreationData.getName());
			}
			else
				log.error("Component [] failed pre-loading step; it will not be available for agent [].", shardClass,
						agentCreationData.getName());
		}
		
		return true;
	}
	
	/**
	 * The method loads all the information necessary for the creation of an agent and returns an {@link Agent} instance
	 * used to manage the life-cycle of the loaded agent.
	 * 
	 * @param agentCreationData
	 *            - the {@link MultiTreeMap}, as loaded at deployment.
	 * @return an {@link Agent} instance for the loaded agent.
	 */
	@Override
	public Agent load(MultiTreeMap agentCreationData)
	{
		CompositeAgent agent = new CompositeAgent();
		for(Object componentObj : agentCreationData.getParameters().getObjects(COMPONENT_PARAMETER_NAME))
			agent.addFeature((AgentShardCore) componentObj);
		return agent;
	}
}
