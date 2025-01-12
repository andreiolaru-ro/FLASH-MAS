package net.xqhs.flash.core.deployment;

import java.util.LinkedList;
import java.util.List;

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

public class Deployment extends Unit {
	{
		// sets logging parameters: the name of the log and the type (which is given by the current platform)
		setUnitName("deployment");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	static Deployment deployment = new Deployment();
	
	String deploymentID = null;
	
	public static Deployment get() {
		return deployment;
	}
	
	public LoadPack getLoadPack(Logger log) {
		return new LoadPack(PlatformUtils.getClassFactory(), deploymentID, log != null ? log : getLogger());
	}
	
	/**
	 * Loads a deployment starting from command line arguments.
	 * <p>
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 * @return the {@link List} of {@link Node} instances that were loadPack.getLoaded().
	 */
	public List<Node> loadDeployment(List<String> args) {
		lf("Loading deployment.");
		
		// load settings & scenario
		DeploymentConfiguration deploymentConfiguration = null;
		try {
			deploymentConfiguration = new DeploymentConfiguration().loadConfiguration(args, true, null);
			lf("Configuration loadPack.getLoaded()");
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
		nodeLoader.configure(null, getLogger(), null);
		for(MultiTreeMap nodeConfig : nodesTrees) {
			lf("Loading node ", EntityIndex.mockPrint(CategoryName.NODE.s(),
					nodeConfig.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME)));
			Node node = nodeLoader.loadNode(nodeConfig, DeploymentConfiguration.filterContext(allEntities,
					nodeConfig.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE)), getLoadPack(null));
			if(node != null) {
				nodes.add(node);
				lf("node loaded: []", node.getName());
			}
			else
				le("node not loaded [].", nodeConfig.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME));
		}
		lf("[] nodes loadPack.getLoaded().", Integer.valueOf(nodes.size()));
		doExit();
		return nodes;
	}
	
	public void loadEntities(MultiTreeMap configuration, Node loadedNode, List<MultiTreeMap> subordinateEntities,
			LoadPack loadPack) {
		lf("Loading order: ", (Object[]) loadPack.getLoadOrder());
		for(String catName : loadPack.getLoadOrder()) {
			CategoryName cat = CategoryName.byName(catName);
			List<MultiTreeMap> entities = DeploymentConfiguration.filterCategoryInContext(subordinateEntities, catName,
					null);
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
							log_kindLoad = "first (" + loadPack.getLoaders().get(catName).keySet().iterator().next()
									+ ")";
						}
					}
				}
				
				// build context
				List<EntityProxy<?>> context = new LinkedList<>();
				if(entityConfig.isSimple(DeploymentConfiguration.CONTEXT_ELEMENT_NAME))
					for(String contextItem : entityConfig.getValues(DeploymentConfiguration.CONTEXT_ELEMENT_NAME))
						if(loadPack.getLoaded().containsKey(contextItem)) {
							if(loadPack.getLoaded().get(contextItem).asContext() != null)
								context.add(loadPack.getLoaded().get(contextItem).asContext());
						}
						else if(!contextItem.equals(loadPack.deploymentID))
							lw("Context item [] for [] []/[]/[] not found as a loaded entity.", contextItem, catName,
									name, kind, local_id);
						
				// build subordinate entities list
				List<MultiTreeMap> subEntities = DeploymentConfiguration.filterContext(subordinateEntities, local_id);
				
				// TODO: provide load() with context and an appropriate list of subordinate entities
				// try to load the entity with a loader
				Entity<?> entity = null;
				if(loaderList != null && !loaderList.isEmpty())
					for(Loader<?> loader : loaderList) { // try loading
						lf("Trying to load []/[] [][] using []th loader for [][]", name, local_id, catName, kind,
								Integer.valueOf(log_nLoader), log_catLoad, log_kindLoad);
						if(loader.preload(entityConfig, context))
							entity = loader.load(entityConfig, context, subEntities);
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
						le("Class for [] []/[]/[] can not be found; tried paths ", catName, name, kind, local_id,
								checkedPaths);
					else {
						lf("Trying to load []/[] [][] using default loader [], from classpath []", name, local_id,
								catName, kind, loadPack.getDefaultLoader().getClass().getName(), cp);
						// add the CP -- will be first
						entityConfig.addFirstValue(SimpleLoader.CLASSPATH_KEY, cp);
					}
					if(loadPack.getDefaultLoader().preload(entityConfig, context))
						entity = loadPack.getDefaultLoader().load(entityConfig, context, subEntities);
				}
				if(entity != null) {
					li("Entity []/[] of type [] successfully loadPack.getLoaded().", name, local_id, catName);
					entityConfig.addSingleValue(DeploymentConfiguration.LOADED_ATTRIBUTE_NAME,
							DeploymentConfiguration.LOADED_ATTRIBUTE_NAME);
					
					loadPack.getLoaded().put(local_id, entity);
					loadedNode.registerEntity(catName, entity, id);
				}
				else
					le("Could not load entity []/[] of type [].", name, local_id, catName);
				lf("loadPack.getLoaded() items:", loadPack.getLoaded().keySet());
			}
		}
	}
}
