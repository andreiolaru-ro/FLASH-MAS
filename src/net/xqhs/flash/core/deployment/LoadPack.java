package net.xqhs.flash.core.deployment;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.DumbLogger;
import net.xqhs.util.logging.Logger;

public class LoadPack {
	protected String									deploymentID;
	protected ClassFactory								classFactory;
	protected List<String>								packages;
	protected Map<String, Map<String, List<Loader<?>>>>	loaders;
	protected Loader<?>									defaultLoader;
	protected Map<String, Entity<?>>					loaded		= new LinkedHashMap<>();
	protected String[]									loadOrder	= new String[] {};
	protected Logger									log;
	
	public LoadPack(ClassFactory classFactory, String deploymentID, Logger log) {
		this.deploymentID = deploymentID;
		this.classFactory = classFactory;
		this.log = log != null ? log : new DumbLogger();
	}
	
	public LoadPack loadFromConfiguration(MultiTreeMap configuration) {
		packages = configuration.getValues(CategoryName.PACKAGE.s());
		
		loaders = new LinkedHashMap<>();
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
						loader.configure(loader_configs.getFirstTree(name), log, classFactory);
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
		defaultLoader.configure(null, log, classFactory);
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
	 * @return the deploymentID
	 */
	public String getDeploymentID() {
		return deploymentID;
	}
	
	/**
	 * @param deploymentID
	 *            the deploymentID to set
	 * @return the {@link LoadPack} instance itself.
	 */
	public LoadPack setDeploymentID(String deploymentID) {
		this.deploymentID = deploymentID;
		return this;
	}
	
	/**
	 * @return the packages
	 */
	public List<String> getPackages() {
		return packages;
	}
	
	/**
	 * @return the loaders
	 */
	public Map<String, Map<String, List<Loader<?>>>> getLoaders() {
		return loaders;
	}
	
	/**
	 * @return the defaultLoader
	 */
	public Loader<?> getDefaultLoader() {
		return defaultLoader;
	}
	
	/**
	 * @return the classFactory
	 */
	public ClassFactory getClassFactory() {
		return classFactory;
	}
	
	/**
	 * @return the loaded
	 */
	public Map<String, Entity<?>> getLoaded() {
		return loaded;
	}
	
	/**
	 * @return the loadOrder
	 */
	public String[] getLoadOrder() {
		return loadOrder;
	}
}
