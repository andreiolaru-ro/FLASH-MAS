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
package net.xqhs.flash.core.node;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.DumbLogger;
import net.xqhs.util.logging.Logger;

/**
 * The {@link NodeLoader} class manages the loading of one node in the system (normally, there is one node per machine,
 * therefore this manages booting FLASH-MAS on the current machine). It manages settings, it loads the scenario, loads
 * the agent definitions (agents are actually created later).
 * <p>
 * After performing all initializations, it creates a {@link Node} instance that manages the actual deployment
 * execution.
 * 
 * @author Andrei Olaru
 */
public class NodeLoader implements Loader<Node> {
	
	protected Logger		l;
	protected ClassFactory	cf;
	
	/**
	 * Functionality not used.
	 */
	@Override
	public boolean configure(MultiTreeMap configuration, Logger log, ClassFactory factory) {
		l = log;
		cf = factory;
		return true;
	}
	
	@Override
	public Node load(MultiTreeMap configuration) {
		return load(configuration, null, null);
	}
	
	/**
	 * Loads one {@link Node} instance, based on the provided configuration.
	 * 
	 * @param context
	 *            - this argument is not used; nodes don't support context.
	 * @return the {@link Node} the was loaded.
	 */
	@Override
	public Node load(MultiTreeMap nodeConfiguration, List<EntityProxy<? extends Entity<?>>> context,
			List<MultiTreeMap> subordinateEntities) {
		if(context != null && context.size() > 0)
			l.lw("nodes don't support context");
		return loadNode(nodeConfiguration, subordinateEntities, null);
	}
	
	/**
	 * Loads one {@link Node} instance, based on the provided configuration.
	 * 
	 * @param nodeConfiguration
	 *            - the configuration.
	 * @param subordinateEntities
	 *            - the entities that should be loaded inside the node, as specified by
	 *            {@link Loader#load(MultiTreeMap, List, List)}.
	 * @param _loadPack
	 *            - pre-configured {@link LoadPack} instance (with class factory and deployment ID. Can be
	 *            <code>null</code>.
	 * @return the {@link Node} the was loaded.
	 */
	public Node loadNode(MultiTreeMap nodeConfiguration, List<MultiTreeMap> subordinateEntities, LoadPack _loadPack) {
		// loader initials
		
		LoadPack loadPack = _loadPack != null ? _loadPack : new LoadPack(cf, null, l);
		loadPack.loadFromConfiguration(nodeConfiguration);
		
		if(l == null)
			l = new DumbLogger("node loader");
		if(loadPack.getClassFactory() == null) {
			l.le("Class factory is null.");
			return null;
		}
		
		// ============================================================================== load entities
		// node instance creation
		if(!nodeConfiguration.isSet(SimpleLoader.CLASSPATH_KEY))
			nodeConfiguration.addOneValue(SimpleLoader.CLASSPATH_KEY, Node.class.getCanonicalName());
		String nodeCatName = CategoryName.NODE.s();
		String nodeName = nodeConfiguration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		List<String> checkedPaths = new LinkedList<>();
		String nodecp = Loader.autoFind(loadPack.getClassFactory(), loadPack.getPackages(),
				nodeConfiguration.get(SimpleLoader.CLASSPATH_KEY), null, null, nodeCatName, checkedPaths);
		if(nodecp == null) {
			l.le("Class for [] [] can not be found; tried paths ", nodeCatName, nodeName, checkedPaths);
			return null;
		}
		nodeConfiguration.addFirstValue(SimpleLoader.CLASSPATH_KEY, nodecp);
		l.lf("Trying to load node using default loader [], from classpath []",
				loadPack.getDefaultLoader().getClass().getName(), CategoryName.NODE.s(), nodecp);
		Node node = (Node) loadPack.getDefaultLoader().load(nodeConfiguration);
		if(node == null) {
			l.le("Could not load [][].", nodeCatName, nodeName);
			return null;
		}
		
		Map<String, Entity<?>> loaded = new LinkedHashMap<>();
		String node_local_id = nodeConfiguration.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE);
		loaded.put(node_local_id, node);
		
		Deployment.get().loadEntities(nodeConfiguration, node, subordinateEntities, loadPack);
		
		if(!nodeConfiguration.containsKey(DeploymentConfiguration.CENTRAL_NODE_KEY))
			return node;
		// delegate the central node
		// and register the central monitoring and control entity in its context
		l.li("Node [] is central node.", node.getName());
		CentralMonitoringAndControlEntity centralEntity = new CentralMonitoringAndControlEntity(new MultiTreeMap()
				.addSingleValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME,
						DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME)
				.addAll(DeploymentConfiguration.CENTRAL_NODE_KEY,
						nodeConfiguration.getValues(DeploymentConfiguration.CENTRAL_NODE_KEY)));
		centralEntity.addGeneralContext(node.nodePylonProxy);
		node.registerEntity(DeploymentConfiguration.MONITORING_TYPE, centralEntity,
				DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
		
		l.lf("Entity [] of type [] registered.", DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME,
				DeploymentConfiguration.MONITORING_TYPE);
		
		l.li("Loading node [] completed.", node.getName());
		return node;
	}
	
	/**
	 * Functionality not used.
	 */
	@Override
	public boolean preload(MultiTreeMap configuration) {
		return true;
	}
	
	/**
	 * Functionality not used.
	 */
	@Override
	public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
		return preload(configuration);
	}
}
