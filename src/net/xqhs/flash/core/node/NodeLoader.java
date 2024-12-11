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
import net.xqhs.flash.core.Entity.EntityIndex;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity;
import net.xqhs.flash.core.node.clientApp.NodeLoaderDecorator;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.Unit;

/**
 * The {@link NodeLoader} class manages the loading of one node in the system (normally, there is one node per machine,
 * therefore this manages booting FLASH-MAS on the current machine). It manages settings, it loads the scenario, loads
 * the agent definitions (agents are actually created later).
 * <p>
 * After performing all initializations, it creates a {@link Node} instance that manages the actual deployment
 * execution.
 * toLoad
 * @author Andrei Olaru
 */
public class NodeLoader extends Unit implements Loader<Node> {
	{
		// sets logging parameters: the name of the log and the type (which is given by the current platform)
		setUnitName("boot");
		setLoggerType(PlatformUtils.platformLogType());
	}

	
	/**
	 * Loads a deployment starting from command line arguments.
	 * <p>
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 * @return the {@link List} of {@link Node} instances that were loaded.
	 */
	public List<Node> loadDeployment(List<String> args) {
		lf("Booting Flash-MAS.");
		
		// load settings & scenario
		DeploymentConfiguration deploymentConfiguration = null;
		try {
			deploymentConfiguration = new DeploymentConfiguration().loadConfiguration(args, true, null);
			lf("Configuration loaded");
		} catch(ConfigLockedException e) {
			le("settings were locked (shouldn't ever happen): " + PlatformUtils.printException(e));
			return null;
		}
		
		List<Node> nodes = new LinkedList<>();
		List<MultiTreeMap> allEntities = deploymentConfiguration.getEntityList();
		List<MultiTreeMap> nodesTrees = DeploymentConfiguration.filterCategoryInContext(allEntities,
				CategoryName.NODE.s(), null);
		if(nodesTrees == null || nodesTrees.isEmpty()) { // the DeploymentConfiguration should have created at least an
															// empty node.
			le("No nodes present in the configuration.");
			return null;
		}
		for(MultiTreeMap nodeConfig : nodesTrees) {
			lf("Loading node ", EntityIndex.mockPrint(CategoryName.NODE.s(),
					nodeConfig.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME)));
			Node node = loadNode(nodeConfig, DeploymentConfiguration.filterContext(allEntities,
					nodeConfig.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE)),
					DeploymentConfiguration.filterCategoryInContext(allEntities, CategoryName.DEPLOYMENT.s(), null)
							.get(0).getAValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE));
			if(node != null) {
				nodes.add(node);
				lf("node loaded: []", node.getName());
			}
			else
				le("node not loaded.");
		}
		lf("[] nodes loaded.", Integer.valueOf(nodes.size()));
		doExit();
		return nodes;
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
			lw("nodes don't support context");
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
	 * @param deploymentID
	 *            - the local ID of the {@link CategoryName#DEPLOYMENT} entity, used to no issue context item errors for
	 *            the deployment itself. Can be <code>null</code>.
	 * @return the {@link Node} the was loaded.
	 */
	public Node loadNode(MultiTreeMap nodeConfiguration, List<MultiTreeMap> subordinateEntities, String deploymentID) {


		// loader initials
		String NAMESEP = DeploymentConfiguration.NAME_SEPARATOR;
		ClassFactory classFactory = PlatformUtils.getClassFactory();
		List<String> checkedPaths = new LinkedList<>(); // used to monitor class paths checked by autoFind().
		
		// ============================================================================== get package list
		List<String> packages = nodeConfiguration.getValues(CategoryName.PACKAGE.s());
		
		// ============================================================================== get loaders
		// loaders are stored as entity -> kind -> loaders
		Map<String, Map<String, List<Loader<?>>>> loaders = new LinkedHashMap<>();
		MultiTreeMap loader_configs = nodeConfiguration.getSingleTree(CategoryName.LOADER.s());
		if(loader_configs != null) {
			if(!loader_configs.getSimpleNames().isEmpty()) // just a warning
				lw("Simple keys from loader tree ignored: ", loader_configs.getSimpleNames());
			for(String name : loader_configs.getHierarchicalNames()) {
				// TODO only the first loader with the name will be loaded
				String entity = null, kind = null;
				if(name.contains(NAMESEP)) {
					entity = name.split(NAMESEP)[0];
					kind = name.split(NAMESEP, 2)[1];
				}
				else
					entity = name;
				if(entity == null || entity.length() == 0)
					le("Loader name parsing failed for []", name);
				
				// find the implementation
				String cp = loader_configs.getDeepValue(name, SimpleLoader.CLASSPATH_KEY);
				cp = Loader.autoFind(classFactory, packages, cp, entity, kind, CategoryName.LOADER.s(), checkedPaths);
				if(cp == null)
					le("Class for loader [] can not be found; tried paths ", name, checkedPaths);
				else { // attach instance to loader map
					try {
						// instantiate loader
						Loader<?> loader = (Loader<?>) classFactory.loadClassInstance(cp, null, true);
						// add to map
						if(!loaders.containsKey(entity))
							loaders.put(entity, new LinkedHashMap<String, List<Loader<?>>>());
						if(!loaders.get(entity).containsKey(kind))
							loaders.get(entity).put(kind, new LinkedList<Loader<?>>());
						loaders.get(entity).get(kind).add(loader);
						// configure // TODO manage with portables
						loader_configs.getFirstTree(name).addAll(CategoryName.PACKAGE.s(), packages);
						loader.configure(loader_configs.getFirstTree(name), getLogger(), classFactory);
						li("Loader for [] of kind [] successfully loaded from [].", entity, kind, cp);
					} catch(Exception e) {
						le("Loader loading failed for []: ", name, PlatformUtils.printException(e));
					}
				}
			}
		}
		else
			li("No loaders configured.");
		
		Loader<?> defaultLoader = new SimpleLoader();
		defaultLoader.configure(null, getLogger(), classFactory);
		if(loaders.containsKey(null)) {
			if(loaders.get(null).containsKey(null) && !loaders.get(null).get(null).isEmpty())
				defaultLoader = loaders.get(null).get(null).get(0);
			else if(!loaders.get(null).isEmpty())
				defaultLoader = loaders.get(null).values().iterator().next().get(0);
		}
		
		// ============================================================================== load entities
		// node instance creation
		if(!nodeConfiguration.isSet(SimpleLoader.CLASSPATH_KEY))
			nodeConfiguration.addOneValue(SimpleLoader.CLASSPATH_KEY, Node.class.getCanonicalName());
		String nodeCatName = CategoryName.NODE.s();
		String nodeName = nodeConfiguration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		String nodecp = Loader.autoFind(classFactory, packages, nodeConfiguration.get(SimpleLoader.CLASSPATH_KEY), null,
				null, nodeCatName, checkedPaths);
		if(nodecp == null) {
			le("Class for [] [] can not be found; tried paths ", nodeCatName, nodeName, checkedPaths);
			return null;
		}
		nodeConfiguration.addFirstValue(SimpleLoader.CLASSPATH_KEY, nodecp);
		lf("Trying to load node using default loader [], from classpath []", defaultLoader.getClass().getName(),
				CategoryName.NODE.s(), nodecp);
		Node node = (Node) defaultLoader.load(nodeConfiguration);
		if(node != null) {
			node.configure1(nodeConfiguration);
		} else {
			le("Could not load [][].", nodeCatName, nodeName);
			return null;
		}

		Map<String, Entity<?>> loaded = new LinkedHashMap<>();
		String node_local_id = nodeConfiguration.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE);
		loaded.put(node_local_id, node);

		String toLoad = nodeConfiguration.getSingleValue(CategoryName.LOAD_ORDER.s());
		if(toLoad == null || toLoad.trim().length() == 0)
			li("Nothing to load");
		else {
			lf("Loading order: ", toLoad);
			List<MessagingPylonProxy> messagingProxies = new LinkedList<>();
			for(String catName : toLoad.split(DeploymentConfiguration.LOAD_ORDER_SEPARATOR)) {
				CategoryName cat = CategoryName.byName(catName);
				List<MultiTreeMap> entities = DeploymentConfiguration.filterCategoryInContext(subordinateEntities,
						catName, null);
				if(entities.isEmpty()) {
					li("No [] entities defined.", catName);
					continue;
				}
				lf("Loading category: ", catName);
				
				for(MultiTreeMap entityConfig : entities) {
					// TODO add comments & notes about what names, kinds and ids really are.
					// try to parse the name / obtain a kind (in order to find an appropriate loader)
					String name = entityConfig.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
					String kind = null, id = null, cp = entityConfig.get(SimpleLoader.CLASSPATH_KEY);
					String local_id = entityConfig.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE);
					if(name != null && name.contains(NAMESEP)) { // if name is can be split, split it into kind and id
						kind = name.split(NAMESEP)[0];
						id = name.split(NAMESEP, 2)[1];
					}
					if(kind == null || kind.length() == 0) {
						if(entityConfig.isSimple(DeploymentConfiguration.KIND_ATTRIBUTE_NAME))
							kind = entityConfig.get(DeploymentConfiguration.KIND_ATTRIBUTE_NAME);
						else if(cat != null && cat.hasNameWithParts())
							kind = entityConfig.get(cat.nameParts()[0]);
					}
					if(id == null || id.length() == 0) {
						if(entityConfig.isSimple(DeploymentConfiguration.NAME_ATTRIBUTE_NAME))
							id = entityConfig.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
						else if(cat != null && cat.hasNameWithParts())
							id = entityConfig.get(cat.nameParts()[1]);
						if(id == null)
							id = name;
					}

					// in case the kind:id format was used, we only want the name to be the id
					if(name != null && name.contains(NAMESEP) && id != null)
						entityConfig.addFirst(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, id);

					// find a loader for the entity
					List<Loader<?>> loaderList = null;
					String log_catLoad = null, log_kindLoad = null;
					int log_nLoader = 0;
					if(loaders.containsKey(catName) && !loaders.get(catName).isEmpty()) {
						// if the category in loader list
						log_catLoad = catName;
						if(loaders.get(catName).containsKey(kind)) { // get loaders for this kind
							loaderList = loaders.get(catName).get(kind);
							log_catLoad = kind;
						}
						else { // if no loaders for this kind
							if(loaders.get(catName).containsKey(null)) {// get the null kind
								loaderList = loaders.get(catName).get(null);
								log_kindLoad = "null";
							}
							else { // get loaders for the first kind
								loaderList = loaders.get(catName).values().iterator().next();
								log_kindLoad = "first (" + loaders.get(catName).keySet().iterator().next() + ")";
							}
						}
					}

					// build context
					List<EntityProxy<?>> context = new LinkedList<>();
					if(entityConfig.isSimple(DeploymentConfiguration.CONTEXT_ELEMENT_NAME))
						for(String contextItem : entityConfig.getValues(DeploymentConfiguration.CONTEXT_ELEMENT_NAME))
							if(loaded.containsKey(contextItem)) {
								if(loaded.get(contextItem).asContext() != null)
									context.add(loaded.get(contextItem).asContext());
							}
							else if(!contextItem.equals(deploymentID))
								lw("Context item [] for [] []/[]/[] not found as a loaded entity.", contextItem,
										catName, name, kind, local_id);

					// build subordinate entities list
					List<MultiTreeMap> subEntities = DeploymentConfiguration.filterContext(subordinateEntities,
							local_id);

					// TODO: provide load() with context and an appropriate list of subordinate entities
					// try to load the entity with a loader

					// Entity<?> entity = null;
//					if(loaderList != null && !loaderList.isEmpty())
//						for(Loader<?> loader : loaderList) { // try loading
//							lf("Trying to load []/[] [][] using []th loader for [][]", name, local_id, catName, kind,
//									Integer.valueOf(log_nLoader), log_catLoad, log_kindLoad);
//							if(loader.preload(entityConfig, context))
//								entity = loader.load(entityConfig, context, subEntities);
//							if(entity != null)
//								break;
//							log_nLoader += 1;
//						}

					Entity<?> entity = loadEntity(node, entityConfig, loaded);

					if (entity != null) {
								li("Entity []/[] of type [] successfully loaded.", name, local_id, catName);
						entityConfig.addSingleValue(DeploymentConfiguration.LOADED_ATTRIBUTE_NAME, DeploymentConfiguration.LOADED_ATTRIBUTE_NAME);
						loaded.put(local_id, entity);
						node.registerEntity(catName, entity, id);
					} else {
						le("Could not load entity []/[] of type [].", name, local_id, catName);

					}


					// if not, try to load the entity with the default loader
					if(entity == null) {
						// attempt to obtain classpath information
						cp = Loader.autoFind(classFactory, packages, cp, kind, id, catName, checkedPaths);
						if(cp == null)
							le("Class for [] []/[]/[] can not be found; tried paths ", catName, name, kind, local_id,
									checkedPaths);
						else {
							lf("Trying to load []/[] [][] using default loader [], from classpath []", name, local_id,
									catName, kind, defaultLoader.getClass().getName(), cp);
							// add the CP -- will be first
							entityConfig.addFirstValue(SimpleLoader.CLASSPATH_KEY, cp);
						}
						if(defaultLoader.preload(entityConfig, context))
							entity = defaultLoader.load(entityConfig, context, subEntities);
					}
					if(entity != null) {
						li("Entity []/[] of type [] successfully loaded.", name, local_id, catName);
						entityConfig.addSingleValue(DeploymentConfiguration.LOADED_ATTRIBUTE_NAME,
								DeploymentConfiguration.LOADED_ATTRIBUTE_NAME);

						// find messaging pylons that can be used by the Node
						EntityProxy<?> ctx = entity.asContext();
						if(ctx != null && ctx instanceof MessagingPylonProxy)
							messagingProxies.add((MessagingPylonProxy) ctx);

						loaded.put(local_id, entity);
						node.registerEntity(catName, entity, id);
					}
					else
						le("Could not load entity []/[] of type [].", name, local_id, catName);
					lf("Loaded items:", loaded.keySet());
				}
			}
			
			lf("Other configuration:");
			
			if(messagingProxies.isEmpty())
				return node;
			MessagingPylonProxy pylon = messagingProxies.stream().findFirst().get();
			node.addGeneralContext(pylon);
			
			if(!nodeConfiguration.containsKey(DeploymentConfiguration.CENTRAL_NODE_KEY))
				return node;
				
			// delegate the central node
			// and register the central monitoring and control entity in its context
			li("Node [] is central node.", node.getName());
			CentralMonitoringAndControlEntity centralEntity = new CentralMonitoringAndControlEntity(
					new MultiTreeMap().addSingleValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME,
							DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME)
							.addAll(DeploymentConfiguration.CENTRAL_NODE_KEY,
									nodeConfiguration.getValues(DeploymentConfiguration.CENTRAL_NODE_KEY)));
			centralEntity.addGeneralContext(pylon);
			node.registerEntity(DeploymentConfiguration.MONITORING_TYPE, centralEntity,
					DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
			
			lf("Entity [] of type [] registered.", DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME,
					DeploymentConfiguration.MONITORING_TYPE);
			
			li("Loading node [] completed.", node.getName());
		}


		return node;
	}

	public Entity<?> loadEntity(Node node, MultiTreeMap entityConfig, Map<String, Entity<?>> loaded) {
		String className = entityConfig.getSingleValue("className");
		try {
			// Assuming `className` points to a concrete subclass of `Entity`
			Class<?> entityClass = Class.forName(className);
			if (Entity.class.isAssignableFrom(entityClass)) {
				return (Entity<?>) entityClass.getDeclaredConstructor().newInstance();
			} else {
				throw new IllegalArgumentException("Class " + className + " is not a valid subclass of Entity.");
			}
		} catch (Exception e) {
			// Handle exceptions properly (e.g., log the error)
			le("Failed to load entity class: " + className, e);
			return null;
		}
	}


	/**
	 * Functionality not used.
	 */
	@Override
	public boolean configure(MultiTreeMap configuration, Logger log, ClassFactory factory) {
		return true;
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
