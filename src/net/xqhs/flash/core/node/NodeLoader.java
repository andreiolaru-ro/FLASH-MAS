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
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.core.util.TreeParameterSet;
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
	 * @param configuration
	 *            - program arguments, passed as a {@link TreeParameterSet} containing one simple key (the name does not
	 *            matter) that is associated with all the arguments.
	 */
	@Override
	public Node load(TreeParameterSet configuration)
	{
		// just get the arguments, they are in the first simple key
		return load(configuration.getValues(configuration.getSimpleKeys().get(0)));
	}
	
	/**
	 * The method handling main functionality of {@link NodeLoader}.
	 * <p>
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 * @return the {@link Node} to manage to deployed system.
	 */
	protected Node load(List<String> args)
	{
		lf("Booting Flash-MAS.");
		// initials
		String NAMESEP = DeploymentConfiguration.NAME_SEPARATOR;
		String ROOT_PACKAGE = DeploymentConfiguration.ROOT_PACKAGE;
		ClassFactory classFactory = PlatformUtils.getClassFactory();
		List<String> checkedPaths = new LinkedList<>(); // used to monitor class paths checked by autoFind().
		
		// ============================================================================== load settings & scenario
		TreeParameterSet deploymentConfiguration = null;
		try
		{
			deploymentConfiguration = new DeploymentConfiguration().loadConfiguration(args, true, null);
			lf("Configuration loaded");
		} catch(ConfigLockedException e)
		{
			le("settings were locked (shouldn't ever happen): " + PlatformUtils.printException(e));
			return null;
		}
		
		// ============================================================================== get general configuration
		TreeParameterSet theConfig = deploymentConfiguration.getTree(CategoryName.CONFIG.getName());
		if(theConfig == null)
			theConfig = new TreeParameterSet();
		
		// ============================================================================== get package list
		List<String> packages = deploymentConfiguration.getValues(CategoryName.PACKAGE.getName());
		
		// ============================================================================== get loaders
		// loaders are stored as entity -> kind -> loaders
		Map<String, Map<String, List<Loader<?>>>> loaders = new HashMap<>();
		TreeParameterSet loader_configs = deploymentConfiguration.getTree(CategoryName.LOADER.getName());
		if(loader_configs != null)
		{
			if(!loader_configs.getSimpleKeys().isEmpty()) // just a warning
				lw("Simple keys from loader tree ignored: ", loader_configs.getSimpleKeys());
			for(String name : loader_configs.getHierarchicalKeys())
			{
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
						Loader<?> loader = (Loader<?>) classFactory.loadClassInstance(cp, null, true);
						if(!loaders.containsKey(entity))
							loaders.put(entity, new HashMap<String, List<Loader<?>>>());
						if(!loaders.get(entity).containsKey(kind))
							loaders.get(entity).put(kind, new LinkedList<Loader<?>>());
						loaders.get(entity).get(kind).add(loader);
						loader.configure(loader_configs.getTree(name), getLogger());
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
		for(CategoryName cat : DeploymentConfiguration.AUTO_LOADS)
		{
			String catName = cat.getName();
			if(deploymentConfiguration.isSimple(catName))
			{
				le("Agent deployment data cannot be empty");
				continue;
			}
			if(deploymentConfiguration.getTree(catName) == null)
				continue;
			TreeParameterSet configs = deploymentConfiguration.getTree(catName);
			if(!configs.getSimpleKeys().isEmpty()) // just a warning
				lw("Simple keys from [] tree ignored: ", cat, configs.getSimpleKeys());
			for(String name : configs.getHierarchicalKeys())
			{
				// try to parse the name / obtain a kind (in order to find an appropriate loader)
				TreeParameterSet config = configs.getTree(name);
				String kind = null, id = null, cp = config.get(SimpleLoader.CLASSPATH_KEY);
				if(name != null && name.contains(NAMESEP))
				{
					kind = name.split(NAMESEP)[0];
					id = name.split(NAMESEP, 2)[1];
				}
				else
					kind = name;
				if(kind == null || kind.length() == 0)
					kind = config.get(cat.nameParts()[0]);
				if(id == null || id.length() == 0)
					id = config.get(cat.nameParts()[1]);
				
				// find a loader
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
				Entity<?> entity = null;
				if(loaderList != null && !loaderList.isEmpty())
					for(Loader<?> loader : loaderList)
					{ // try loading
						lf("Trying to load [][] using []th loader for [][]", catName, kind, new Integer(log_nLoader),
								log_catLoad, log_kindLoad);
						if(loader.preload(config))
							entity = loader.load(config);
						if(entity != null)
							break;
						log_nLoader += 1;
					}
				if(entity == null)
				{
					lf("Trying to load [][] using default loader [], from classpath []", catName, kind,
							defaultLoader.getClass().getName(), cp);
					// attempt to obtain classpath information
					cp = autoFind(classFactory, packages, cp, ROOT_PACKAGE, kind, id, cat.getName(), checkedPaths);
					if(cp == null)
						le("Class for [] []/[] can not be found; tried paths ", catName, name, kind, checkedPaths);
					else
						configs.getTree(name).set(SimpleLoader.CLASSPATH_KEY, cp);
					if(defaultLoader.preload(config))
						entity = defaultLoader.load(config);
				}
				if(entity != null)
					li("Entity [] of type [] loaded.", name, catName);
				else
					le("Could not load entity [] of type [].", name, catName);
			}
		}
		
		// get agents
		
		doExit();
		
		return null;
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
	public boolean configure(TreeParameterSet configuration, Logger log)
	{
		return true;
	}
	
	/**
	 * Functionality not used.
	 */
	@Override
	public boolean preload(TreeParameterSet configuration)
	{
		return true;
	}
}
