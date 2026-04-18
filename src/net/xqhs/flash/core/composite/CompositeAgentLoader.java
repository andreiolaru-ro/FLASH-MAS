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
package net.xqhs.flash.core.composite;

import java.util.LinkedList;
import java.util.List;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * Agent loader for agents extending {@link CompositeAgent}.
 * <p>
 * The choice of using a specialized loader as opposed to using the default loader and doing all the loading inside the
 * composite agent was made so as to decouple dynamic class loading, as well as calls to
 * {@link ClassFactory#loadClassInstance} from the actual implementation of {@link CompositeAgent}.
 * 
 * @author Andrei Olaru
 */
public class CompositeAgentLoader implements Loader<Agent> {
	/**
	 * The name of attributes containing entity names.
	 */
	protected static final String	NAME_ATTRIBUTE_NAME		= DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
	/**
	 * Name of XML nodes in the scenario representing shards.
	 */
	protected static final String	SHARD_NODE_NAME			= "shard";
	/**
	 * The name of the attribute representing the class of the shard in the shard node. The class may not be specified,
	 * it the shard is standard and its class is specified by the corresponding {@link StandardAgentShard} entry.
	 */
	protected static final String	SHARD_CLASS_PARAMETER	= SimpleLoader.CLASSPATH_KEY;
	
	/**
	 * The packages configured in the deployment.
	 */
	protected List<String>		packages;
	/**
	 * The simple loader to use in order to load custom CompositeAgent instances, if it is the case.
	 */
	protected Loader<Entity<?>>	privateSimpleLoaderInstance	= new SimpleLoader();
	/**
	 * The {@link LoadPack} instance to use.
	 */
	protected LoadPack			lp;
	
	@Override
	public boolean configure(MultiTreeMap config, LoadPack loadPack) {
		lp = loadPack;
		// TODO clone lp and add new packages
		packages = lp.getPackages();
		packages.addAll(config.getValues(CategoryName.PACKAGE.s()));
		privateSimpleLoaderInstance.configure(config, lp);
		return true;
	}
	
	@Override
	public boolean preload(MultiTreeMap configuration) {
		return preload(configuration, null);
	}
	
	/**
	 * The method checks potential problems that could appear in the creation of an agent, as specified by the
	 * information in the argument. Potential problems relate, for example, to inexistent classes for shards.
	 * <p>
	 * The method creates the necessary {@link AgentShard} instances and pre-loads them.
	 * <p>
	 * If the agent will surely not be able to load, <code>false</code> will be returned. For any non-fatal issues, the
	 * method should return <code>true</code> and output warnings in the lp.
	 * 
	 * @param agentConfiguration
	 *            - the {@link MultiTreeMap}, as loaded from the deployment file.
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	@Override
	public boolean preload(MultiTreeMap agentConfiguration, List<EntityProxy<? extends Entity<?>>> context) {
		String lpPre = (agentConfiguration.isSimple(NAME_ATTRIBUTE_NAME) ? agentConfiguration.get(NAME_ATTRIBUTE_NAME)
				: "<agent>") + ": ";
		if(agentConfiguration.isSet(SimpleLoader.CLASSPATH_KEY))
			if(!privateSimpleLoaderInstance.preload(agentConfiguration, context))
				return false;
		if(agentConfiguration.isSet(SHARD_NODE_NAME))
			for(String shardName : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTreeKeys()) {
				for(MultiTreeMap shardConfig : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTrees(shardName)) {
					preloadShard(shardName, shardConfig, context, lpPre);
				}
			}
		
		return true;
	}
	
	/**
	 * Pre-loads an {@link AgentShard} (checks that the shard class can be loaded.
	 * 
	 * @param shardName
	 *            - the name of the shard.
	 * @param shardConfig
	 *            - the configuration of the shard.
	 * @param context
	 *            - the context of the agent containing the shard.
	 * @param lpPre
	 *            - prefix to add to lp entries.
	 */
	public void preloadShard(String shardName, MultiTreeMap shardConfig, List<EntityProxy<? extends Entity<?>>> context,
			String lpPre) {
		// get shard class
		String shardClass = shardConfig == null ? null : shardConfig.getSingleValue(SHARD_CLASS_PARAMETER);
		List<String> checked = new LinkedList<>();
		// test given class, if any
		if(shardClass != null)
			shardClass = Loader.autoFind(lp.getClassFactory(), packages, shardClass, null, null, null, checked);
		if(shardClass == null) {
			if(shardName == null) {
				lp.le(lpPre + "Shard has neither name nor class specified. Shard will not be available.");
				return;
			}
			AgentShardDesignation shardDesignation = AgentShardDesignation.autoDesignation(shardName);
			
			if(context != null)
				for(EntityProxy<?> contextEntity : context)
					if(contextEntity instanceof PylonProxy) {
						String recommendedClass = ((PylonProxy) contextEntity)
								.getRecommendedShardImplementation(shardDesignation);
						if(recommendedClass != null) {
							lp.lf("Pylon [] recommends [] shard at classpath [].", contextEntity.getEntityName(),
									shardName, recommendedClass);
							shardClass = recommendedClass;
							break;
						}
						lp.lf("Pylon [] does not recommend a []/[] shard.", contextEntity.getEntityName(), shardName,
								shardDesignation);
					}
			if(shardClass == null)
				shardClass = Loader.autoFind(lp.getClassFactory(), packages, null, shardName, null,
						CategoryName.SHARD.s(),
						checked);
		}
		if(shardClass == null)
			shardClass = Loader.autoFind(lp.getClassFactory(), packages, shardName, shardName, null,
					CategoryName.SHARD.s(),
					checked);
		if(shardClass == null) {
			lp.le(lpPre
					+ "Shard class not specified / not found for shard []. Shard will not be available. Checked paths: ",
					shardName, checked);
			return;
		}
		
		if(lp.getClassFactory().canLoadClass(shardClass)) {
			lp.lf(lpPre + "shard [" + shardName + "] can be loaded");
			if(shardConfig != null) {
				if(shardConfig.containsKey(SHARD_CLASS_PARAMETER))
					shardConfig.removeKey(SHARD_CLASS_PARAMETER); // workaround lacking addFirstValue
				shardConfig.setValue(SHARD_CLASS_PARAMETER, shardClass); // changes the type of the parameter
			}
		}
		else {
			lp.le(lpPre + "Shard class [" + shardName + " | " + shardClass + "] not found; it will not be loaded.");
			return;
		}
	}
	
	/**
	 * This method calls {@link #load(MultiTreeMap, List, List)}.
	 */
	@Override
	public Agent load(MultiTreeMap agentConfiguration) {
		return load(agentConfiguration, null, null);
	}
	
	/**
	 * The method loads all the information necessary for the creation of an agent and returns an {@link Agent} instance
	 * used to manage the life-cycle of the loaded agent.
	 * 
	 * @param agentConfiguration
	 *            - the {@link MultiTreeMap}, as loaded at deployment.
	 * @return an {@link Agent} instance for the loaded agent.
	 */
	@Override
	public Agent load(MultiTreeMap agentConfiguration, List<EntityProxy<? extends Entity<?>>> context,
			List<MultiTreeMap> subordinateEntities) {
		String lpPre = (agentConfiguration.isSimple(NAME_ATTRIBUTE_NAME) ? agentConfiguration.get(NAME_ATTRIBUTE_NAME)
				: "<agent>") + ": ";
		CompositeAgentModel agent = null;
		if(agentConfiguration.isSet(SimpleLoader.CLASSPATH_KEY)) {
			// agent should be loaded from a class path
			Entity<?> built = privateSimpleLoaderInstance.load(agentConfiguration);
			if(built instanceof CompositeAgentModel)
				agent = (CompositeAgentModel) built;
			else
				return (Agent) lp.lr(null,
						lpPre + "Instance built from classpath [] not instance of CompositeAgentModel",
						agentConfiguration.getValue(SimpleLoader.CLASSPATH_KEY));
			
		}
		else
			agent = createAgentInstance(agentConfiguration);
		String agentName = agentConfiguration.getValue(NAME_ATTRIBUTE_NAME);
		
		if(context != null)
			for(EntityProxy<?> contextItem : context)
				agent.addGeneralContext(contextItem);
			
		if(agentConfiguration.isSet(SHARD_NODE_NAME))
			for(String shardName : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTreeKeys())
				for(MultiTreeMap shardConfig : agentConfiguration.getSingleTree(SHARD_NODE_NAME).getTrees(shardName)) {
					AgentShard shard = loadShard(shardName, shardConfig, lpPre, agentName);
					if(shard != null)
						agent.addShard(shard);
				}
		return agent;
	}
	
	/**
	 * Creates a new instance of {@link CompositeAgent}. This can be overridden to use a different class.
	 * 
	 * @param configuration
	 *            - the configuration for the agent.
	 * @return the agent instance.
	 */
	@SuppressWarnings("static-method")
	protected CompositeAgentModel createAgentInstance(MultiTreeMap configuration) {
		return new CompositeAgent(configuration);
	}
	
	/**
	 * Loads an {@link AgentShard} to be added to a {@link CompositeAgent}.
	 * 
	 * @param shardName
	 *            - the name of the shard.
	 * @param shardConfig_arg
	 *            - the configuration of the shard.
	 * @param lpPre
	 *            - prefix to add to lp entries.
	 * @param agentName
	 *            - the name of the agent that will contain the shard, used for logging only.
	 * @return the loaded shard, if successful, <code>null</code> otherwise.
	 */
	public AgentShard loadShard(String shardName, MultiTreeMap shardConfig_arg, String lpPre, String agentName) {
		MultiTreeMap shardConfig = shardConfig_arg == null ? new MultiTreeMap() : shardConfig_arg;
		String shardClass = shardConfig.getSingleValue(SHARD_CLASS_PARAMETER);
		shardConfig.addAll(CategoryName.PACKAGE.s(), packages);
		if(shardClass != null)
			try {
				AgentShard shard = (AgentShard) lp.getClassFactory().loadClassInstance(shardClass, null, true);
				lp.lf(lpPre + "Shard [] created for agent [] from classpath []. now configuring.", shardName,
						agentName, shardClass);
				if(shard.configure(shardConfig))
					lp.lf(lpPre + "Shard [] for agent [] configured.", shardName, agentName);
				else
					lp.le(lpPre + "Shard [] for agent [] configuration failed.", shardName, agentName);
				return shard;
			} catch(Exception e) {
				lp.le(lpPre + "Shard [] failed to load (from []); it will not be available for agent []:", shardName,
						shardClass, agentName, PlatformUtils.printException(e));
				return null;
			}
		return null;
	}
}
