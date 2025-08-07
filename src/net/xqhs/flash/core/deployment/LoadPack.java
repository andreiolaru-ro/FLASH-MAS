package net.xqhs.flash.core.deployment;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.ConfigurableEntity;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.config.Config;
import net.xqhs.util.logging.Debug.DebugItem;
import net.xqhs.util.logging.DumbLogger;
import net.xqhs.util.logging.Logger;

/**
 * This structure contains information that is useful in loading new entities. This should be used by {@link Loader}
 * instances to correctly load entities and integrate them in the correct context.
 * <p>
 * A {@link LoadPack} instance is created for each node, and is passed down in {@link Loader} instances. After the
 * initial configuration (via #loadFromConfiguration(MultiTreeMap)), the instance is not expected to change, but at
 * least some of the {@link Loader} configuration (with this {@link LoadPack} instance) is done <b>before</b> the
 * configuration of this load pack is completed.
 * <p>
 * The class also implements {@link Logger} so that it can be used for logging directly.
 * <p>
 * {@link LoadPack} extends {@link Config} so it can be {@link #lock}ed. If further configuration is needed, it can be
 * {@link #clone}d as a new instance.
 * 
 * @author Andrei Olaru
 */
public class LoadPack extends Config implements Logger {
	/**
	 * The ID of the current deployment. Can be <code>null</code>.
	 */
	protected String									deploymentID;
	/**
	 * The {@link ClassFactory} instance to use. If never <code>null</code>. If not configured otherwise, the default is
	 * used, as recommended by {@link PlatformUtils}.
	 */
	protected ClassFactory								classFactory;
	/**
	 * Package information -- package names in which to look for classes.
	 */
	protected List<String>								packages	= new LinkedList<>();
	/**
	 * The {@link Loader} instances created so far that can be used to load entities.
	 * <p>
	 * The structure is index by entity type -> entity kind -> list of available loaders.
	 * <p>
	 * Appropriate loaders (depending on type and, if available, kind) should be tried in order until one succeeds.
	 */
	protected Map<String, Map<String, List<Loader<?>>>>	loaders		= new LinkedHashMap<>();
	/**
	 * The default loader to use for entities in case no appropriate loader is found or none succeeds.
	 */
	protected Loader<?>									defaultLoader;
	/**
	 * The types of entities to be loaded when loading a node.
	 */
	protected String[]									loadOrder	= new String[] {};
	/**
	 * The {@link Logger} instance to use for logging messages. Is never <code>null</code>.
	 */
	protected Logger									log;
	
	/**
	 * Creates a {@link LoadPack} instance with the minimum necessary information.
	 * 
	 * @param classFactory
	 *            - the {@link ClassFactory} to use. If <code>null</code>, the default will be used, as recommended by
	 *            {@link PlatformUtils}.
	 * @param deploymentID
	 *            - ID of the deployment. Can be <code>null</code>.
	 * @param log
	 *            - the {@link Logger} to use. if <code>null</code>, a new instance of {@link DumbLogger} will be used.
	 */
	public LoadPack(ClassFactory classFactory, String deploymentID, Logger log) {
		this.deploymentID = deploymentID;
		this.classFactory = classFactory != null ? classFactory : PlatformUtils.getClassFactory();
		this.log = log != null ? log : new DumbLogger();
	}
	
	/**
	 * Fills some of the fields by reading a the configuration of the deployment.
	 * <p>
	 * It loads:
	 * <ul>
	 * <li>packages
	 * <li>loaders (plus it {@link ConfigurableEntity#configure}s the loaders) with current information.
	 * <li>the load order.
	 * </ul>
	 * 
	 * @param configuration
	 *            - the configuration of the deployment.
	 * @return this instance itself.
	 * @throws ConfigLockedException
	 *             if the {@link LoadPack} cannot be modified anymore (e.g. it is already configured for a node or
	 *             another entity).
	 */
	public LoadPack loadFromConfiguration(MultiTreeMap configuration) throws ConfigLockedException {
		locked();
		if(configuration == null)
			return this;
		packages.addAll(configuration.getValues(CategoryName.PACKAGE.s()));
		
		MultiTreeMap loader_configs = configuration.getSingleTree(CategoryName.LOADER.s());
		if(loader_configs != null) {
			if(!loader_configs.getSimpleNames().isEmpty()) // just a warning
				log.lw("Simple keys from loader tree ignored: ", loader_configs.getSimpleNames());
			for(String name : loader_configs.getHierarchicalNames()) {
				// TODO only the first loader with the name will be loaded
				String entity = null, kind = null;
				if(name.contains(DeploymentConfiguration.NAME_SEPARATOR)) {
					entity = name.split(DeploymentConfiguration.NAME_SEPARATOR)[0];
					kind = name.split(DeploymentConfiguration.NAME_SEPARATOR, 2)[1];
				}
				else
					entity = name;
				if(entity == null || entity.length() == 0)
					log.le("Loader name parsing failed for []", name);
				
				// find the implementation
				String cp = loader_configs.getDeepValue(name, SimpleLoader.CLASSPATH_KEY);
				List<String> checkedPaths = new LinkedList<>();
				cp = Loader.autoFind(classFactory, packages, cp, entity, kind, CategoryName.LOADER.s(), checkedPaths);
				if(cp == null)
					log.le("Class for loader [] can not be found; tried paths ", name, checkedPaths);
				else { // attach instance to loader map
					try {
						// instantiate loader
						Loader<?> loader = (Loader<?>) classFactory.loadClassInstance(cp, null, true);
						// add to map
						if(!loaders.containsKey(entity))
							loaders.put(entity, new LinkedHashMap<>());
						if(!loaders.get(entity).containsKey(kind))
							loaders.get(entity).put(kind, new LinkedList<>());
						loaders.get(entity).get(kind).add(loader);
						// configure // TODO manage with portables
						loader_configs.getFirstTree(name).addAll(CategoryName.PACKAGE.s(), packages);
						loader.configure(loader_configs.getFirstTree(name), this);
						log.li("Loader for [] of kind [] successfully loaded from [].", entity, kind, cp);
					} catch(Exception e) {
						log.le("Loader loading failed for []: ", name, PlatformUtils.printException(e));
					}
				}
			}
		}
		else
			log.li("No loaders configured.");
		
		defaultLoader = new SimpleLoader();
		defaultLoader.configure(null, this);
		if(loaders.containsKey(null)) {
			if(loaders.get(null).containsKey(null) && !loaders.get(null).get(null).isEmpty())
				defaultLoader = loaders.get(null).get(null).get(0);
			else if(!loaders.get(null).isEmpty())
				defaultLoader = loaders.get(null).values().iterator().next().get(0);
		}
		
		String toLoad = configuration.getSingleValue(CategoryName.LOAD_ORDER.s());
		if(toLoad == null || toLoad.trim().length() == 0)
			log.li("Load order is none or empty [].", toLoad);
		else
			loadOrder = toLoad.split(DeploymentConfiguration.LOAD_ORDER_SEPARATOR);
		return this;
	}
	
	/**
	 * @return the deploymentID (see {@link #deploymentID})
	 */
	public String getDeploymentID() {
		return deploymentID;
	}
	
	/**
	 * @return the packages (see {@link #packages})
	 */
	public List<String> getPackages() {
		return packages;
	}
	
	/**
	 * @return the loaders (see {@link #loaders})
	 */
	public Map<String, Map<String, List<Loader<?>>>> getLoaders() {
		return loaders;
	}
	
	/**
	 * @return the defaultLoader (see {@link #defaultLoader})
	 */
	public Loader<?> getDefaultLoader() {
		return defaultLoader;
	}
	
	/**
	 * @return the classFactory (see {@link #classFactory})
	 */
	public ClassFactory getClassFactory() {
		return classFactory;
	}
	
	/**
	 * @return the loadOrder (see {@link #loadOrder})
	 */
	public String[] getLoadOrder() {
		return loadOrder;
	}
	
	/**
	 * Creates a new {@link LoadPack} instance which acts as a clone of this instance.
	 * 
	 * @return the new {@link LoadPack} instance.
	 */
	public LoadPack getClone() {
		LoadPack clone = new LoadPack(classFactory, deploymentID, log);
		clone.packages.addAll(packages);
		for(Map.Entry<String, Map<String, List<Loader<?>>>> entry : loaders.entrySet()) {
			Map<String, List<Loader<?>>> kindMap = new LinkedHashMap<>();
			for(Map.Entry<String, List<Loader<?>>> kindEntry : entry.getValue().entrySet()) {
				kindMap.put(kindEntry.getKey(), new LinkedList<>(kindEntry.getValue()));
			}
			clone.loaders.put(entry.getKey(), kindMap);
		}
		clone.defaultLoader = defaultLoader;
		clone.loadOrder = loadOrder.clone();
		return clone;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
	public void le(String message, Object... arguments) {
		log.le(message, arguments);
	}
	
	@Override
	public void lw(String message, Object... arguments) {
		log.lw(message, arguments);
	}
	
	@Override
	public void li(String message, Object... arguments) {
		log.li(message, arguments);
	}
	
	@Override
	public void lf(String message, Object... arguments) {
		log.lf(message, arguments);
	}
	
	@Override
	public Object lr(Object ret) {
		return log.lr(ret);
	}
	
	@Override
	public Object lr(Object ret, String message, Object... arguments) {
		return log.lr(ret, message, arguments);
	}
	
	@Override
	public boolean ler(boolean ret, String message, Object... arguments) {
		return log.ler(ret, message, arguments);
	}
	
	@Override
	public void dbg(DebugItem debug, String message, Object... arguments) {
		log.dbg(debug, message, arguments);
	}
}
