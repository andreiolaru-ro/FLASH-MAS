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
package net.xqhs.flash.core.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
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
 * 
 * @author Andrei Olaru
 */
public class NodeLoader extends Unit implements Loader<Node>
{
	{
		// sets logging parameters: the name of the log and the type (which is given by the current platform)
		setUnitName("boot").setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * Loads a deployment starting from command line arguments.
	 * <p>
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 * @return the {@link List} of {@link Node} instances that were loaded.
	 */
	public List<Node> loadDeployment(List<String> args)
	{
		lf("Booting Flash-MAS.");
		
		// ============================================================================== load settings & scenario
		MultiTreeMap deploymentConfiguration = null;
		try
		{
			deploymentConfiguration = new DeploymentConfiguration().loadConfiguration(args, true, null);
			lf("Configuration loaded");
		} catch(ConfigLockedException e)
		{
			le("settings were locked (shouldn't ever happen): " + PlatformUtils.printException(e));
			return null;
		}
		
		List<Node> nodes = new LinkedList<>();
		
		MultiTreeMap nodesTrees = deploymentConfiguration.getSingleTree(CategoryName.NODE.getName());
		for(String nodeName : nodesTrees.getTreeKeys())
		{
			int index = 0;
			for(MultiTreeMap nodeConfig : nodesTrees.getTrees(nodeName))
			{
				lf("Loading node ", (nodeName != null ? nodeName : "<noname>")
						+ (nodesTrees.getTrees(nodeName).size() > 1 ? "#" + index : ""));
				Node node = load(nodeConfig, null);
				if(node != null)
					nodes.add(node);
				index += 1;
			}
		}
		
		doExit();
		return nodes;
	}
	
	/**
	 * Loads one {@link Node} instance, based on the provided configuration.
	 * 
	 * @param nodeConfiguration
	 *            - the configuration.
	 * @param context
	 *            - this argument is not used; nodes don't support context.
	 * @return the {@link Node} the was loaded.
	 */
	@Override
	public Node load(MultiTreeMap nodeConfiguration, List<Entity<?>> context)
	{
		if(context != null && context.size() > 0)
			lw("nodes don't support context");
		return load(nodeConfiguration);
	}
	
	/**
	 * Loads one {@link Node} instance, based on the provided configuration.
	 * 
	 * @param nodeConfiguration
	 *            - the configuration.
	 * @return the {@link Node} the was loaded.
	 */
	@Override
	public Node load(MultiTreeMap nodeConfiguration)
	{
		// node instance creation
		Node node = new Node(nodeConfiguration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME));
		
		// loader initials
		String NAMESEP = DeploymentConfiguration.NAME_SEPARATOR;
		String ROOT_PACKAGE = DeploymentConfiguration.ROOT_PACKAGE;
		ClassFactory classFactory = PlatformUtils.getClassFactory();
		List<String> checkedPaths = new LinkedList<>(); // used to monitor class paths checked by autoFind().
		
		// ============================================================================== get package list
		List<String> packages = nodeConfiguration.getValues(CategoryName.PACKAGE.getName());
		
		// ============================================================================== get loaders
		// loaders are stored as entity -> kind -> loaders
		Map<String, Map<String, List<Loader<?>>>> loaders = new HashMap<>();
		MultiTreeMap loader_configs = nodeConfiguration.getSingleTree(CategoryName.LOADER.getName());
		if(loader_configs != null)
		{
			if(!loader_configs.getSimpleNames().isEmpty()) // just a warning
				lw("Simple keys from loader tree ignored: ", loader_configs.getSimpleNames());
			for(String name : loader_configs.getHierarchicalNames())
			{
				// TODO only the first loader with the name will be loaded
				String entity = null, kind = null;
				if(name.contains(NAMESEP))
				{
					entity = name.split(NAMESEP)[0];
					kind = name.split(NAMESEP, 2)[1];
				}
				else
					entity = name;
				if(entity == null || entity.length() == 0)
					le("Loader name parsing failed for []", name);
				
				// find the implementation
				String cp = loader_configs.getDeepValue(name, SimpleLoader.CLASSPATH_KEY);
				cp = autoFind(classFactory, packages, cp, ROOT_PACKAGE, entity, kind, CategoryName.LOADER.getName(),
						checkedPaths);
				if(cp == null)
					le("Class for loader [] can not be found; tried paths ", name, checkedPaths);
				else
				{ // attach instance to loader map
					try
					{
						// instantiate loader
						Loader<?> loader = (Loader<?>) classFactory.loadClassInstance(cp, null, true);
						// add to map
						if(!loaders.containsKey(entity))
							loaders.put(entity, new HashMap<String, List<Loader<?>>>());
						if(!loaders.get(entity).containsKey(kind))
							loaders.get(entity).put(kind, new LinkedList<Loader<?>>());
						loaders.get(entity).get(kind).add(loader);
						// configure // TODO manage with portables
						loader_configs.getFirstTree(name).addAll(CategoryName.PACKAGE.getName(), packages);
						loader.configure(loader_configs.getFirstTree(name), getLogger());
						li("Loader for [] of kind [] successfully loaded from [].", entity, kind, cp);
					} catch(Exception e)
					{
						le("Loader loading failed for []: ", name, PlatformUtils.printException(e));
					}
				}
			}
		}
		else
			li("No loaders configured.");
		
		Loader<?> defaultLoader = new SimpleLoader();
		defaultLoader.configure(null, getLogger());
		if(loaders.containsKey(null))
		{
			if(loaders.get(null).containsKey(null) && !loaders.get(null).get(null).isEmpty())
				defaultLoader = loaders.get(null).get(null).get(0);
			else if(!loaders.get(null).isEmpty())
				defaultLoader = loaders.get(null).values().iterator().next().get(0);
		}
		
		// ============================================================================== load entities
		String[] toLoad = nodeConfiguration.getSingleValue(CategoryName.LOAD_ORDER.getName())
				.split(DeploymentConfiguration.LOAD_ORDER_SEPARATOR);
		li("Loading: ", (Object[]) toLoad);
		for(String catName : toLoad)
		{
			CategoryName cat = CategoryName.byName(catName);
			if(!nodeConfiguration.containsKey(catName))
			{
				li("No [] entities defined.", catName);
				continue;
			}
			if(nodeConfiguration.isSimple(catName))
			{
				le("Deployment data cannot be empty for [].", catName);
				continue;
			}
			if(!nodeConfiguration.isSingleton(catName))
			{
				le("Category node for [] must be a singleton value.", catName);
				continue;
			}
			if(!nodeConfiguration.containsKey(catName) || nodeConfiguration.getSingleTree(catName) == null)
				continue;
			MultiTreeMap catTree = nodeConfiguration.getSingleTree(catName);
			if(!catTree.getSimpleNames().isEmpty()) // just a warning
				lw("Simple keys from [] tree ignored: ", cat, catTree.getSimpleNames());
			for(String name : catTree.getHierarchicalNames())
			{
				List<MultiTreeMap> entities = new LinkedList<>();
				if(catTree.isSingleton(catName))
					entities.add(catTree.getSingleTree(name));
				else
					entities = catTree.getTrees(name);
				for(MultiTreeMap entityConfig : entities)
				{
					// try to parse the name / obtain a kind (in order to find an appropriate loader)
					String kind = null, id = null, cp = entityConfig.get(SimpleLoader.CLASSPATH_KEY);
					if(name != null && name.contains(NAMESEP))
					{ // if name is splittable, split it into kind and id
						kind = name.split(NAMESEP)[0];
						id = name.split(NAMESEP, 2)[1];
					}
					if(kind == null || kind.length() == 0)
					{
						if(entityConfig.isSimple(DeploymentConfiguration.KIND_ATTRIBUTE_NAME))
							kind = entityConfig.get(DeploymentConfiguration.KIND_ATTRIBUTE_NAME);
						else if(cat.hasNameWithParts())
							kind = entityConfig.get(cat.nameParts()[0]);
						if(kind == null)
							kind = name; // was in the implementation not sure is a good idea
					}
					if(id == null || id.length() == 0)
					{
						if(entityConfig.isSimple(DeploymentConfiguration.NAME_ATTRIBUTE_NAME))
							id = entityConfig.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
						else if(cat.hasNameWithParts())
							id = entityConfig.get(cat.nameParts()[1]);
						if(id == null)
							id = name;
					}
					
					// find a loader for the enitity
					List<Loader<?>> loaderList = null;
					String log_catLoad = null, log_kindLoad = null;
					int log_nLoader = 0;
					if(loaders.containsKey(catName) && !loaders.get(catName).isEmpty())
					{ // if the category in loader list
						log_catLoad = catName;
						if(loaders.get(catName).containsKey(kind))
						{ // get loaders for this kind
							loaderList = loaders.get(catName).get(kind);
							log_catLoad = kind;
						}
						else
						{ // if no loaders for this kind
							if(loaders.get(catName).containsKey(null))
							{// get the null kind
								loaderList = loaders.get(catName).get(null);
								log_kindLoad = "null";
							}
							else
							{ // get loaders for the first kind
								loaderList = loaders.get(catName).values().iterator().next();
								log_kindLoad = "first(" + loaders.get(catName).keySet().iterator().next() + ")";
							}
						}
					}
					// try to load the entity with a loader
					Entity<?> entity = null;
					if(loaderList != null && !loaderList.isEmpty())
						for(Loader<?> loader : loaderList)
						{ // try loading
							lf("Trying to load [][] using []th loader for [][]", catName, kind,
									new Integer(log_nLoader), log_catLoad, log_kindLoad);
							if(loader.preload(entityConfig))
								entity = loader.load(entityConfig);
							if(entity != null)
								break;
							log_nLoader += 1;
						}
					// if not, try to load the entity with the default loader
					if(entity == null)
					{
						// attempt to obtain classpath information
						cp = autoFind(classFactory, packages, cp, ROOT_PACKAGE, kind, id, cat.getName(), checkedPaths);
						if(cp == null)
							le("Class for [] []/[] can not be found; tried paths ", catName, name, kind, checkedPaths);
						else
						{
							lf("Trying to load [][] using default loader [], from classpath []", catName, kind,
									defaultLoader.getClass().getName(), cp);
							// add the CP -- will be first if no other is provided // TODO
							entityConfig.addOneValue(SimpleLoader.CLASSPATH_KEY, cp);
						}
						if(defaultLoader.preload(entityConfig))
							entity = defaultLoader.load(entityConfig);
					}
					if(entity != null)
						li("Entity [] of type [] loaded.", name, catName);
					else
						le("Could not load entity [] of type [].", name, catName);
				}
			}
		}
		return node;
	}
	
	/**
	 * Makes the first letter of the given string upper-case.
	 * 
	 * @param s
	 *            - the string.
	 * @return the string with the first letter converted to upper-case.
	 */
	static String capitalize(String s)
	{
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	/**
	 * Attempts to find a specific class given some known information about it. It searches the class in the following
	 * sequence:
	 * <ul>
	 * <li>verify directly the given classpath (<code>given_cp</code>)
	 * <li>verify if the given classpath can be found in any of the packages
	 * <li>verify combinations of:
	 * <ul>
	 * <li>the <code>root_package</code>, the {@link DeploymentConfiguration#CORE_PACKAGE} in the
	 * <code>root_package</code> or any of the packages
	 * <li>with
	 * <li>combinations of package paths formed of the <code>upper_name</code> and the <code>lower_name</code>
	 * <li>with
	 * <li>the given classpath or class names created by joining <code>upper_name</code> and <code>entity</code>;
	 * <code>lower_name</code> and <code>entity</code>; or <code>lower_name</code>, <code>upper_name</code>, and
	 * <code>entity</code>.
	 * </ul>
	 * </ul>
	 * <p>
	 * TODO: example
	 * 
	 * @param factory
	 *            - the {@link ClassFactory} that can test if the class exists / can be loaded.
	 * @param packages
	 *            - a list of java packages in which to search.
	 * @param given_cp
	 *            - a classpath or a class name that may be given directly, saving the effort of searching for the
	 *            class. This classpath will also be searched in the list of packages.
	 * @param root_package
	 *            - the root package in which to search.
	 * @param upper_name
	 *            - the upper name in the kind hierarchy of the entity (should not be <code>null</code>).
	 * @param lower_name
	 *            - the upper name in the kind hierarchy of the entity (can be <code>null</code>).
	 * @param entity
	 *            - the name of the entity for which a class is searched (should not be <code>null</code>).
	 * @param checkedPaths
	 *            - a {@link List} in which all checked paths will be added (checked paths are classpaths where the
	 *            class have been searched).
	 * @return the full classpath of the first class that has been found, if any; <code>null</code> otherwise.
	 */
	protected static String autoFind(ClassFactory factory, List<String> packages, String given_cp, String root_package,
			String upper_name, String lower_name, String entity, List<String> checkedPaths)
	{
		String D = ".";
		checkedPaths.clear();
		checkedPaths.add(given_cp);
		if(given_cp != null && factory.canLoadClass(given_cp))
			return given_cp;
		if(packages != null)
			for(String p : packages)
			{
				checkedPaths.add(p + D + given_cp);
				if(factory.canLoadClass(p + D + given_cp))
					return p + D + given_cp;
			}
		List<String> clsNames = new LinkedList<>();
		if(given_cp != null)
			clsNames.add(given_cp);
		if(upper_name == null)
			return null;
		clsNames.add(capitalize(upper_name) + capitalize(entity));
		if(lower_name != null)
		{
			clsNames.add(capitalize(lower_name) + capitalize(entity));
			clsNames.add(capitalize(lower_name) + capitalize(upper_name) + capitalize(entity));
		}
		List<String> roots = new ArrayList<>();
		roots.add(root_package);
		roots.add(root_package + D + DeploymentConfiguration.CORE_PACKAGE);
		roots.addAll(packages);
		for(String cls : clsNames)
			for(String r : roots)
			{
				checkedPaths.add(r + D + upper_name + D + cls);
				if(lower_name != null)
				{
					checkedPaths.add(r + D + upper_name + D + lower_name + D + cls);
					checkedPaths.add(r + D + lower_name + D + upper_name + D + cls);
					checkedPaths.add(r + D + lower_name + D + cls);
				}
			}
		for(String p : checkedPaths)
			if(factory.canLoadClass(p))
				return p;
		return null;
	}
	
	/**
	 * Functionality not used.
	 */
	@Override
	public boolean configure(MultiTreeMap configuration, Logger log)
	{
		return true;
	}
	
	/**
	 * Functionality not used.
	 */
	@Override
	public boolean preload(MultiTreeMap configuration)
	{
		return true;
	}
	
	/**
	 * Functionality not used.
	 */
	@Override
	public boolean preload(MultiTreeMap configuration, List<Entity<?>> context)
	{
		return preload(configuration);
	}
}
