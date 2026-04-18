package net.xqhs.flash.core.deployment;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityIndex;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.Unit;

/**
 * Represents a deployment of entities.
 * <p>
 * While in theory it is an entity, it does not implement the {@link Entity} interface, as it is a <i>virtual</i>
 * entity, it is not referenced by other entities and cannot be started, stopped or added in the context of other
 * entities.
 */
public class Deployment extends Unit {
	{
		// sets logging parameters: the name of the log and the type (which is given by the current platform)
		setUnitName("deployment");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * Singleton instance of the deployment.
	 */
	static Deployment deployment = new Deployment();
	
	/**
	 * The ID of the deployment.
	 */
	String deploymentID = null;
	
	/**
	 * @return the singleton instance of the deployment.
	 */
	public static Deployment get() {
		return deployment;
	}
	
	/**
	 * Creates a basic {@link LoadPack} using the current deployment ID and a given {@link Logger}.
	 * 
	 * @param log
	 *            - the logger to use, or <code>null</code> to use the default logger.
	 * @return a new {@link LoadPack} instance that can be used to load entities.
	 */
	public LoadPack getBasicLoadPack(Logger log) {
		return new LoadPack(PlatformUtils.getClassFactory(), deploymentID, log != null ? log : getLogger());
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
		lf("Loading deployment.");
		
		// load settings & scenario
		DeploymentConfiguration deploymentConfiguration = null;
		try {
			deploymentConfiguration = new DeploymentConfiguration().loadConfiguration(args, true, null);
			lf("Configuration loaded");
		} catch(ConfigLockedException e) {
			le("settings were locked (shouldn't ever happen): ", PlatformUtils.printException(e));
			return null;
		}
		
		// get configurations for individual nodes
		List<Node> nodes = new LinkedList<>();
		List<MultiTreeMap> allEntities = deploymentConfiguration.getEntityList();
		List<MultiTreeMap> nodesTrees = DeploymentConfiguration.filterCategoryInContext(allEntities,
				CategoryName.NODE.s(), null);
		if(nodesTrees == null || nodesTrees.isEmpty()) { // the DeploymentConfiguration should have created at least an
															// empty node.
			le("No nodes present in the configuration.");
			return null;
		}
		// load each node in the deployment
		deploymentID = DeploymentConfiguration.filterCategoryInContext(allEntities, CategoryName.DEPLOYMENT.s(), null)
				.get(0).getAValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE);
		NodeLoader nodeLoader = new NodeLoader();
		nodeLoader.configure(null, getBasicLoadPack(null));
		for(MultiTreeMap nodeConfig : nodesTrees) {
			lf("Loading node ", EntityIndex.mockPrint(CategoryName.NODE.s(),
					nodeConfig.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME)));
			Node node = nodeLoader.load(nodeConfig, null, DeploymentConfiguration.filterContext(allEntities,
					nodeConfig.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE)));
			if(node != null) {
				nodes.add(node);
				lf("node loaded: []", node.getName());
			}
			else
				le("node not loaded [].", nodeConfig.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME));
		}
		lf("[] nodes loaded.", Integer.valueOf(nodes.size()));
		doExit();
		return nodes;
	}
	
	/**
	 * Loads a set of entities in the given node, using the provided {@link LoadPack} instance.
	 * <p>
	 * if provided, the loaded entities will be added to the given map of loaded entities.
	 * 
	 * @param entitiesToLoad
	 *            - the entities to load, as a list of {@link MultiTreeMap} entity configurations
	 * @param hostNode
	 *            - the node in which the entities will be loaded
	 * @param loadPack
	 *            - the {@link LoadPack} instance containing the loaders and other configuration
	 * @param loadedEntities
	 *            - the already loaded entities, to which the newly loaded entities will be added
	 * @return a {@link List} of newly loaded entities
	 */
	public List<Entity<?>> loadEntities(List<MultiTreeMap> entitiesToLoad, Node hostNode, LoadPack loadPack,
			Map<String, Entity<?>> loadedEntities) {
		lf("Loading node [] Loading order: ", hostNode.getName(), loadPack.getLoadOrder());
		List<Entity<?>> newLoadedEntities = new LinkedList<>();
		Map<String, Entity<?>> loaded = loadedEntities != null ? loadedEntities : new HashMap<>();
		for(String catName : loadPack.getLoadOrder()) {
			List<MultiTreeMap> entities = DeploymentConfiguration.filterCategoryInContext(entitiesToLoad, catName,
					null);
			if(entities.isEmpty()) {
				li("No [] entities defined.", catName);
				continue;
			}
			lf("Loading category [] with previously loaded items ", catName, loaded.keySet());
			
			for(MultiTreeMap entityConfig : entities) {
				String name = entityConfig.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME),
						local_id = entityConfig.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE);
				
				// build context
				List<EntityProxy<?>> context = new LinkedList<>();
				if(entityConfig.isSimple(DeploymentConfiguration.CONTEXT_ELEMENT_NAME))
					for(String contextItem : entityConfig.getValues(DeploymentConfiguration.CONTEXT_ELEMENT_NAME))
						if(loaded.containsKey(contextItem))
							try {
								if(loaded.get(contextItem).asContext() != null)
									context.add(loaded.get(contextItem).asContext());
							} catch(UnsupportedOperationException e) {
								lw("Entity [] cannot be used as context [].", loaded.get(contextItem).getName(),
										PlatformUtils.printException(e));
							}
						else if(!contextItem.equals(loadPack.deploymentID))
							lw("Context item [] for [] []/[] not found as a loaded entity in", contextItem, catName,
									name, local_id,
									loaded.entrySet().stream().map(e -> e.getKey() + "|" + e.getValue().getName())
											.collect(Collectors.joining(",")));
				SimpleEntry<String, Entity<?>> res = loadEntity(catName, entityConfig, entitiesToLoad, context,
						loadPack);
				if(res.getValue() != null) {
					Entity<?> entity = res.getValue();
					loaded.put(local_id, entity);
					hostNode.registerEntity(catName, entity, res.getKey());
					newLoadedEntities.add(entity);
				}
			}
		}
		lf("Loaded items:", newLoadedEntities);
		return newLoadedEntities;
	}
	
	/**
	 * 
	 * @param entitiesToLoad
	 *            - the entities to load, as a list of {@link MultiTreeMap} entity configurations
	 * @param loadPack
	 *            - the {@link LoadPack} instance containing the loaders and other configuration
	 * @param parentContext
	 *            - when calling this method from a loader, the context in which the parent entity is loaded. If this is
	 *            not <code>null</code>, <code>loaded entities</code> will not be used.
	 * @return a {@link List} of newly loaded entities
	 */
	public List<Entity<?>> loadEntities(List<MultiTreeMap> entitiesToLoad, LoadPack loadPack,
			List<EntityProxy<? extends Entity<?>>> parentContext) {
		List<Entity<?>> newLoadedEntities = new LinkedList<>();
		for(MultiTreeMap entityConfig : entitiesToLoad) {
			String catName = entityConfig.getAValue(DeploymentConfiguration.CATEGORY_ATTRIBUTE_NAME);
			SimpleEntry<String, Entity<?>> res = loadEntity(catName, entityConfig, entitiesToLoad, parentContext,
					loadPack);
			if(res.getValue() != null)
				newLoadedEntities.add(res.getValue());
		}
		return newLoadedEntities;
	}
	
	/**
	 * Loads one entity.
	 * 
	 * @param catName
	 *            - the category name of the entity to load
	 * @param entityConfig
	 *            - the configuration of the entity to load
	 * @param entitiesToLoad
	 *            - the full list of entities to load, from which to extract subordinate entities
	 * @param context
	 *            - the context in which the entity is loaded
	 * @param loadPack
	 *            - the {@link LoadPack} instance containing the loaders and other configuration
	 * @return the loaded entity, or <code>null</code> if the entity could not be loaded.
	 */
	protected SimpleEntry<String, Entity<?>> loadEntity(String catName, MultiTreeMap entityConfig,
			List<MultiTreeMap> entitiesToLoad, List<EntityProxy<? extends Entity<?>>> context, LoadPack loadPack) {
		// TODO add comments & notes about what names, kinds and ids really are.
		// try to parse the name / obtain a kind (in order to find an appropriate loader)
		CategoryName cat = CategoryName.byName(catName);
		String name = entityConfig.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME),
				local_id = entityConfig.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE);
		String kind = null, id = null, cp = entityConfig.get(SimpleLoader.CLASSPATH_KEY);
		if(name != null && name.contains(DeploymentConfiguration.NAME_SEPARATOR)) {
			// if name is can be split, split it into kind and id
			kind = name.split(DeploymentConfiguration.NAME_SEPARATOR)[0];
			id = name.split(DeploymentConfiguration.NAME_SEPARATOR, 2)[1];
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
		if(name != null && name.contains(DeploymentConfiguration.NAME_SEPARATOR) && id != null)
			entityConfig.addFirst(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, id);
		
		// find a loader for the entity
		List<Loader<?>> loaderList = null;
		String log_catLoad = null, log_kindLoad = null;
		int log_nLoader = 0;
		if(loadPack.getLoaders().containsKey(catName) && !loadPack.getLoaders().get(catName).isEmpty()) {
			// if the category in loader list
			log_catLoad = catName;
			if(loadPack.getLoaders().get(catName).containsKey(kind)) { // get loadPack.getLoaders() for this
																		// kind
				loaderList = loadPack.getLoaders().get(catName).get(kind);
				log_catLoad = kind;
			}
			else { // if no loadPack.getLoaders() for this kind
				if(loadPack.getLoaders().get(catName).containsKey(null)) {// get the null kind
					loaderList = loadPack.getLoaders().get(catName).get(null);
					log_kindLoad = "null";
				}
				else { // get loadPack.getLoaders() for the first kind
					loaderList = loadPack.getLoaders().get(catName).values().iterator().next();
					log_kindLoad = "first (" + loadPack.getLoaders().get(catName).keySet().iterator().next() + ")";
				}
			}
		}
		
		// build subordinate entities list
		List<MultiTreeMap> subEntities = DeploymentConfiguration.filterContext(entitiesToLoad, local_id);
		
		// TODO: provide load() with context and an appropriate list of subordinate entities
		// try to load the entity with a loader
		Entity<?> entity = null;
		if(loaderList != null && !loaderList.isEmpty())
			for(Loader<?> loader : loaderList) { // try loading
				lf("Trying to load []/[] [][] using []th loader for [][]", name, local_id, catName, kind,
						Integer.valueOf(log_nLoader), log_catLoad, log_kindLoad);
				if(loader.preload(entityConfig, context))
					entity = loader.load(entityConfig, new ArrayList<>(context), subEntities);
				if(entity != null)
					break;
				log_nLoader += 1;
			}
		// if not, try to load the entity with the default loader
		if(entity == null) {
			// attempt to obtain classpath information
			List<String> checkedPaths = new LinkedList<>();
			cp = Loader.autoFind(loadPack.getClassFactory(), loadPack.getPackages(), cp, kind, id, catName,
					checkedPaths);
			if(cp == null)
				le("Class for [] []/[]/[] can not be found; tried paths ", catName, name, kind, local_id, checkedPaths);
			else {
				lf("Trying to load []/[] [][] using default loader [], from classpath []", name, local_id, catName,
						kind, loadPack.getDefaultLoader().getClass().getName(), cp);
				// add the CP -- will be first
				entityConfig.addFirstValue(SimpleLoader.CLASSPATH_KEY, cp);
			}
			if(loadPack.getDefaultLoader().preload(entityConfig, context))
				entity = loadPack.getDefaultLoader().load(entityConfig, context, subEntities);
		}
		if(entity != null) {
			li("Entity []/[] of type [] successfully loaded.", name, local_id, catName);
			entityConfig.addSingleValue(DeploymentConfiguration.LOADED_ATTRIBUTE_NAME,
					DeploymentConfiguration.LOADED_ATTRIBUTE_NAME);
		}
		else
			le("Could not load entity []/[] of type [].", name, local_id, catName);
		return new SimpleEntry<>(id, entity);
	}
}
