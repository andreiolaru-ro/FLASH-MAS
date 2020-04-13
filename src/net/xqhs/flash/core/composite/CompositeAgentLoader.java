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

import java.util.List;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
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
	 * The name of attributes containing entity names.
	 */
	protected static final String	NAME_ATTRIBUTE_NAME		= DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
	/**
	 * Name of XML nodes in the scenario representing shards.
	 */
	private static final String		SHARD_NODE_NAME			= "shard";
	/**
	 * The name of the attribute representing the class of the shard in the shard node. The class may not be specified,
	 * it the shard is standard and its class is specified by the corresponding {@link StandardAgentShard} entry.
	 */
	private static final String		SHARD_CLASS_PARAMETER	= SimpleLoader.CLASSPATH_KEY;
	
	/**
	 * Logger to use during the loading process.
	 */
	Logger							log;
	/**
	 * The {@link ClassFactory} instance to use for loading classes.
	 */
	ClassFactory					classLoader;
	/**
	 * The packages configured in the deployment.
	 */
	List<String>					packages;
	
	@Override
	public boolean configure(MultiTreeMap config, Logger loaderLog, ClassFactory classFactory)
	{
		log = loaderLog;
		classLoader = classFactory;
		packages = config.getValues(CategoryName.PACKAGE.s());
		return true;
	}
	
	@Override
	public boolean preload(MultiTreeMap configuration)
	{
		return preload(configuration, null);
	}
	
	/**
	 * The method checks potential problems that could appear in the creation of an agent, as specified by the
	 * information in the argument. Potential problems relate, for example, to inexistent classes for shards.
	 * <p>
	 * The method creates the necessary {@link AgentShard} instances and pre-loads them.
	 * <p>
	 * If the agent will surely not be able to load, <code>false</code> will be returned. For any non-fatal issues, the
	 * method should return <code>true</code> and output warnings in the log.
	 * 
	 * @param agentConfiguration
	 *                               - the {@link MultiTreeMap}, as loaded from the deployment file.
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	@Override
	public boolean preload(MultiTreeMap agentConfiguration, List<EntityProxy<? extends Entity<?>>> context)
	{
		String logPre = (agentConfiguration.isSimple(NAME_ATTRIBUTE_NAME) ? agentConfiguration.get(NAME_ATTRIBUTE_NAME)
				: "<agent>") + ": ";
		if(agentConfiguration.isSet(SHARD_NODE_NAME))
			for(String shardName : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTreeKeys())
			{
				for(MultiTreeMap shardConfig : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTrees(shardName))
				{
					// get shard class
					String shardClass = shardConfig.get(SHARD_CLASS_PARAMETER);
					// test given class, if any
					if(shardClass != null)
						shardClass = Loader.autoFind(classLoader, packages, shardClass, null, null, null, null);
					if(shardClass == null)
					{
						if(shardName == null)
						{
							log.error(logPre
									+ "Shard has neither name nor class specified. Shard will not be available.");
							continue;
						}
						AgentShardDesignation shardDesignation = AgentShardDesignation.autoDesignation(shardName);
						
						if(context != null)
							for(EntityProxy<?> contextEntity : context)
								if(contextEntity instanceof PylonProxy)
								{
									String recommendedClass = ((PylonProxy) contextEntity)
											.getRecommendedShardImplementation(shardDesignation);
									if(recommendedClass != null)
									{
										log.trace("Pylon [] recommends [] shard at classpath [].",
												contextEntity.getEntityName(), shardName, recommendedClass);
										shardClass = recommendedClass;
										break;
									}
									log.trace("Pylon [] does not recommend a []/[] shard.",
											contextEntity.getEntityName(), shardName, shardDesignation);
								}
						if(shardClass == null)
							shardClass = Loader.autoFind(classLoader, packages, null, shardName, null,
									CategoryName.SHARD.s(), null);
					}
					if(shardClass == null)
						shardClass = Loader.autoFind(classLoader, packages, shardName, shardName, null,
								CategoryName.SHARD.s(), null);
					if(shardClass == null)
					{
						log.error(logPre + "Shard class not specified / not found for shard [" + shardName
								+ "]. Shard will not be available.");
						continue;
					}
					
					if(classLoader.canLoadClass(shardClass))
					{
						log.trace(logPre + "shard [" + shardName + "] can be loaded");
						if(shardConfig.containsKey(SHARD_CLASS_PARAMETER))
							shardConfig.removeKey(SHARD_CLASS_PARAMETER); // workaround lacking addFirstValue
						shardConfig.setValue(SHARD_CLASS_PARAMETER, shardClass); // changes the type of the parameter
					}
					else
					{
						log.error(logPre + "Shard class [" + shardName + " | " + shardClass
								+ "] not found; it will not be loaded.");
						continue;
					}
				}
			}
		
		return true;
	}
	
	/**
	 * This method calls {@link #load(MultiTreeMap, List, List)}.
	 */
	@Override
	public Agent load(MultiTreeMap agentConfiguration)
	{
		return load(agentConfiguration, null, null);
	}
	
	/**
	 * The method loads all the information necessary for the creation of an agent and returns an {@link Agent} instance
	 * used to manage the life-cycle of the loaded agent.
	 * 
	 * @param agentConfiguration
	 *                               - the {@link MultiTreeMap}, as loaded at deployment.
	 * @return an {@link Agent} instance for the loaded agent.
	 */
	@Override
	public Agent load(MultiTreeMap agentConfiguration, List<EntityProxy<? extends Entity<?>>> context,
			List<MultiTreeMap> subordinateEntities)
	{
		String logPre = (agentConfiguration.isSimple(NAME_ATTRIBUTE_NAME) ? agentConfiguration.get(NAME_ATTRIBUTE_NAME)
				: "<agent>") + ": ";
		CompositeAgent agent = new CompositeAgent(agentConfiguration);
		String agentName = agentConfiguration.getValue(NAME_ATTRIBUTE_NAME);
		
		if(context != null)
			for(EntityProxy<?> contextItem : context)
				agent.addGeneralContext(contextItem);
			
		if(agentConfiguration.isSet(SHARD_NODE_NAME))
			for(String shardName : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTreeKeys())
				for(MultiTreeMap shardConfig : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTrees(shardName))
				{
					String shardClass = shardConfig.getSingleValue(SHARD_CLASS_PARAMETER);
					if(shardClass != null)
						try
						{
							AgentShard shard = (AgentShard) classLoader.loadClassInstance(shardClass, null, true);
							log.trace(logPre + "Shard [] created for agent [] from classpath []. now configuring.",
									shardName, agentName, shardClass);
							if(shard.configure(shardConfig))
								log.trace(logPre + "Shard [] for agent [] configured.", shardName, agentName);
							else
								log.error(logPre + "Shard [] for agent [] configuration failed.", shardName, agentName);
							agent.addShard(shard);
						} catch(Exception e)
						{
							log.error(logPre
									+ "Shard [] failed to load (from []); it will not be available for agent []:",
									shardName, shardClass, agentName, PlatformUtils.printException(e));
							continue;
						}
				}
		return agent;
	}
}
