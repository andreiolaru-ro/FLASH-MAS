package net.xqhs.flash.abms;

import java.util.ArrayList;
import java.util.List;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Manages a group of entities.
 * 
 * @author Andrei Olaru
 */
public class EntityGroup extends EntityCore<Node> {
	/**
	 * 
	 */
	public static class EntityGroupLoader implements Loader<EntityGroup> {
		protected static final String	ENTITY_NUMBER_PARAM_NAME	= "n";
		/**
		 * The load pack that will be used to load the entities.
		 */
		protected LoadPack				lp;
		protected List<MultiTreeMap>	entityConfigurations		= null;
		protected String				baseEntityName				= null;
		
		@Override
		public boolean configure(MultiTreeMap configuration, LoadPack loadPack) {
			lp = loadPack;
			return true;
		}
		
		@Override
		public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
			int n = Integer.parseInt(configuration.get(ENTITY_NUMBER_PARAM_NAME));
			entityConfigurations = new ArrayList<>(n);
			// we support only one subcateg
			// do we support multiple trees per category?
			String cat = configuration.getTreeKeys().get(0);
			
			lp.lf("Loading n: [] entities of category: ", Integer.valueOf(n), cat);
			MultiTreeMap subConfig = configuration.getATree(cat)
					.getFirstTree(configuration.getATree(cat).getTreeKeys().get(0));
			// retain the base name, but then the name will be cleared from the sub-configuration, so that entity names
			// can be assigned cleanly by the specific loader
			baseEntityName = subConfig.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			subConfig.clear(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			// lp.lf(subConfig.toString());
			for(int i = 0; i < n; i++)
				entityConfigurations.add(subConfig.copyShallow());
			return true;
		}
		
		@Override
		public boolean preload(MultiTreeMap configuration) {
			return preload(configuration, null);
		}
		
		@Override
		public EntityGroup load(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context,
				List<MultiTreeMap> subordinateEntities) {
			preload(configuration, context);
			
			List<EntityProxy<? extends Entity<?>>> extendedContext = new ArrayList<>(context);
			// extendedContext.add(simulation.asContext());
			
			List<Entity<?>> entities = Deployment.get().loadEntities(entityConfigurations, lp, extendedContext);
			EntityGroup ag = new EntityGroup(entities);
			ag.configure(configuration);
			
			return ag;
		}
		
		@Override
		public EntityGroup load(MultiTreeMap configuration) {
			return load(configuration, null, null);
		}
	}
	
	/**
	 * The serial UID
	 */
	private static final long	serialVersionUID	= 1L;
	/**
	 * The entities that are part of this group.
	 */
	protected List<Entity<?>>	entities;
	
	/**
	 * Creates a new entity group with the given entities.
	 * 
	 * @param entityList
	 *            - the list of entities that are part of this group.
	 */
	public EntityGroup(List<Entity<?>> entityList) {
		entities = entityList;
	}
	
	protected void display() {
	}
	
}
