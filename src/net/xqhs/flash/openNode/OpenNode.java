package net.xqhs.flash.openNode;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Implements a {@link Node} which is able to integrate new entities at runtime. New entities can be added via
 * {@link #dynamicLoad}.
 */
public class OpenNode extends Node {
	/**
	 * The configuration must be retained, to use when loading further entities.
	 */
	protected MultiTreeMap nodeConfiguration;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		nodeConfiguration = configuration;
		return super.configure(configuration);
	}
	
	/**
	 * Loads the entities in the given deployment tree, a tree like the one produced by {@link DeploymentConfiguration}.
	 * 
	 * @param deploymentTree
	 *            - a {@link MultiTreeMap} containing configurations for each entity, with entities grouped by category;
	 *            and an entry with the key {@link DeploymentConfiguration#LOCAL_ID_ATTRIBUTE} containing a flat list of
	 *            all entities to deploy.
	 */
	protected void dynamicLoad(MultiTreeMap deploymentTree) {
		li("to insert:", deploymentTree);
		LoadPack pack = Deployment.get().getBasicLoadPack(getLogger());
		try {
			pack.loadFromConfiguration(nodeConfiguration);
			pack.loadFromConfiguration(deploymentTree);
		} catch(ConfigLockedException e) {
			// cannot happen, as the configuration is new.
		}
		Map<String, Entity<?>> loaded = new LinkedHashMap<>();
		registeredEntities.forEach((category, entities) -> entities.forEach(e -> loaded.put(e.getName(), e)));
		LinkedList<MultiTreeMap> entitiesConfig = new LinkedList<>();
		for(String entityName : deploymentTree.getSingleTree(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE)
				.getHierarchicalNames())
			entitiesConfig.add(
					deploymentTree.getSingleTree(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE).getSingleTree(entityName));
		List<Entity<?>> entities = Deployment.get().loadEntities(entitiesConfig, this, pack, loaded);
		startAndRegister(entities, false);
	}
}
