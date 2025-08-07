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
package net.xqhs.flash.core;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * A loader instance has the capability of creating new {@link Entity} instances.
 * <p>
 * Entities are created based on a configuration that is loaded at boot time and is represented as a
 * {@link MultiTreeMap}.
 * <p>
 * For many types of entities the loader may not be necessary. If instantiating the class if sufficient,
 * {@link SimpleLoader} may be used.
 * 
 * @param <T>
 *                the type of {@link Entity} instance that the loader can load.
 * 
 * @author andreiolaru
 */
public interface Loader<T extends Entity<?>>
{
	/**
	 * Configures an instance of the loader based on deployment data.
	 * 
	 * @param configuration
	 *            - configuration for this loader, if any.
	 * @param loadPack
	 *            - the information to use for loading entities.
	 * @return <code>true</code> if the configuration process was successful and the {@link Loader} instance is ready to
	 *         load entities; <code>false</code> if this instance cannot be expected to work normally.
	 */
	public boolean configure(MultiTreeMap configuration, LoadPack loadPack);
	
	/**
	 * Same as {@link #preload(MultiTreeMap)}, but performs the checks in the given context.
	 * 
	 * @param configuration
	 *            - the configuration data for the entity to load.
	 * @param context
	 *            - the entities that form the context of the entity to be loaded. The argument may be <code>null</code>
	 *            or empty.
	 * @return <code>true</code> if {@link #load}ing the entity is expected to complete successfully; <code>false</code>
	 *         if the entity cannot load with the given configuration.
	 * @see #preload(MultiTreeMap)
	 */
	public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context);
	
	/**
	 * Performs checks and completes the configuration.
	 * <p>
	 * This method <i>may</i> be implemented by implementing classes in order to
	 * <ul>
	 * <li>Check that an {@link Entity} of type <code>T</code> and with the given configuration can indeed be loaded; it
	 * this method returns <code>true</code>, then calling {@link #load} with the same configuration is expected to
	 * complete successfully.
	 * <li>Add new elements to the given configuration so as to improve the subsequent performance of a call to
	 * {@link #load} with the same configuration.
	 * </ul>
	 * Depending on each specific {@link Loader} implementation, the call to {@link #preload} may be optional or
	 * mandatory, but it is recommended that {@link #load} checks if the configuration has been pre-loaded and, if not,
	 * to call {@link #preload}.
	 * 
	 * @param configuration
	 *                          - the configuration of the entity that one intends to load. This COnfiguration may be
	 *                          modified (added to) in this method.
	 * @return <code>true</code> if {@link #load}ing the entity is expected to complete successfully; <code>false</code>
	 *         if the entity cannot load with the given configuration.
	 */
	public boolean preload(MultiTreeMap configuration);
	
	/**
	 * Loads a new instance of entity <b>T</b> and provides it with the given context, as well as with a flat (as
	 * opposed to the hierarchical structure provided by the configuration) list of entities that should be present in
	 * the context of this entity. The list is expected to be formed similarly to the result of
	 * {@link DeploymentConfiguration#getEntityList()} and to be able to be processed using
	 * {@link DeploymentConfiguration#filterContext(List, String)} and
	 * {@link DeploymentConfiguration#filterCategoryInContext(List, String, String)}. Both the
	 * <code>configuration</code> and the <code>subordinateEntities</code> list should contain references to the same
	 * {@link MultiTreeMap} instances.
	 * <p>
	 * Note that some of the subordinate entities in the configuration and the subordinateEntities list may contain the
	 * attribute {@link DeploymentConfiguration#LOADED_ATTRIBUTE_NAME}, indicating that the entity has already been
	 * loaded and does not need to be loaded anymore.
	 * 
	 * @param configuration
	 *            - the configuration data for the entity to load.
	 * @param context
	 *            - the entities that form the context of the loaded entity. The argument may be <code>null</code> or
	 *            empty.
	 * @param subordinateEntities
	 *            - a flat list of entities that should be loaded inside the loaded entity. This may be
	 *            <code>null</code>.
	 * @return the entity, if loading has been successful.
	 */
	public T load(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context,
			List<MultiTreeMap> subordinateEntities);
	
	/**
	 * Loads a new instance of entity <b>T</b>, without providing it with any context.
	 * <p>
	 * See also {@link #load(MultiTreeMap, List, List)}.
	 * 
	 * @param configuration
	 *                          - the configuration data for the entity.
	 * @return the entity, if loading has been successful.
	 */
	public T load(MultiTreeMap configuration);
	
	/**
	 * Type of item to be searched for by <code>autoFind</code> methods.
	 * 
	 * @author Andrei Olaru
	 */
	public enum SearchItemType {
		/**
		 * File.
		 */
		FILE,
		/**
		 * Class.
		 */
		CLASS
	}
	
	/**
	 * Attempts to find a specific class given some known information about it. Among this information there are 3
	 * conceptual elements:
	 * <ul>
	 * <li>the type of the entity or similar concept (e.g. agent, pylon, loader etc); usually it is the last word in the
	 * class name, e.g. Composite<i>Agent</i>, SimpleAgent<i>Loader</i>, etc;
	 * <li>the "upper" name -- the kind of the entity, and normally it should be given (it should not be
	 * <code>null</code>); e.g. <i>composite</i> (for agents), <i>local</i> or <i>websocket</i> (for pylons), etc;
	 * <li>the "lower" name -- a more specific name, which may be omitted (can be <code>null</code>); e.g. in
	 * <i>Sequential</i>CompositeAgent;
	 * </ul>
	 * 
	 * The method searches the class in the following sequence:
	 * <ul>
	 * <li>verify directly the given classpath (<code>given_cp</code>)
	 * <li>verify if the given classpath can be found in any of the packages
	 * <li>verify all combinations of three elements -- the base package, a specific subtree, and the classpath, where:
	 * <ul>
	 * <li>the base package is one of {@link DeploymentConfiguration#ROOT_PACKAGE},
	 * {@link DeploymentConfiguration#CORE_PACKAGE}, and any of the given <code>packages</code>.
	 * <li>the subtree is a classpath segment formed as one of the following (e.g. the <code>base</code> package and for
	 * the <i>sequential-composite-agent</i> lower-upper-entity search case):
	 * <ul>
	 * <li>the upper name -- e.g. <code>base.composite.</code>
	 * <li>the upper name and the lower name -- e.g. <code>base.composite.sequential.</code>
	 * <li>the lower name and the upper name -- e.g. <code>base.sequential.composite.</code>
	 * <li>the lower name -- e.g. <code>base.sequential.</code>
	 * </ul>
	 * <li>the classpath is any camel-case rendition of the following combination (e.g. for the
	 * <i>sequential-composite-agent</i> lower-upper-entity search case):
	 * <ul>
	 * <li>upper name and entity -- e.g. CompositeAgent
	 * <li>lower name and entity -- e.g. SequentialAgent
	 * <li>lower name, upper name, and entity -- e.g. SequentialCompositeAgent
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
	 * @param upper_name
	 *            - the upper name in the kind hierarchy of the entity (should not be <code>null</code> if the
	 *            <code>given_cp</code> is <code>null</code>). When attempting to use it as a package name,
	 *            {@link #toPackageName(String)} is used.
	 * @param lower_name
	 *            - the upper name in the kind hierarchy of the entity (can be <code>null</code>). When attempting to
	 *            use it as a package name, {@link #toPackageName(String)} is used.
	 * @param entity
	 *            - the name of the entity for which a class is searched (should not be <code>null</code>).
	 * @param checkedPaths
	 *            - a {@link List} in which all checked paths will be added (checked paths are classpaths where the
	 *            class have been searched).
	 * @return the full classpath of the first class that has been found, if any; <code>null</code> otherwise.
	 */
	static String autoFind(ClassFactory factory, List<String> packages, String given_cp, String upper_name,
			String lower_name, String entity, List<String> checkedPaths)
	{
		return autoFind(packages, given_cp, upper_name, lower_name, entity, checkedPaths, SearchItemType.CLASS,
				factory);
	}
	
	/**
	 * Same as {@link #autoFind(ClassFactory, List, String, String, String, String, List)}, but searching for files
	 * instead of classes. The workings are the same, except that in the package names dots are substituted by slashes.
	 * The <code>checkedPaths</code> and the method return value also use slashes.
	 * 
	 * @see #autoFind(ClassFactory, List, String, String, String, String, List)
	 * 
	 *      TODO: upper_name, lower_name and entity don't work in a meaningful way and are assembled by capitalization
	 *      of the names. The to-do is to change the manner of assembly when searching for files and assemble via
	 *      underscores.
	 * 
	 * @param packages
	 * @param given_cp
	 * @param upper_name
	 * @param lower_name
	 * @param entity
	 * @param checkedPaths
	 * @return the path to the first file found, if any; <code>null</code> otherwise.
	 */
	static String autoFind(List<String> packages, String given_cp, String upper_name, String lower_name, String entity,
			List<String> checkedPaths) {
		return autoFind(packages, given_cp, upper_name, lower_name, entity, checkedPaths, SearchItemType.FILE);
	}
	
	/**
	 * Checks if a {@link SearchItemType} item can be found.
	 * 
	 * @param path
	 *            - the path to check.
	 * @param searchType
	 *            - the type of item.
	 * @param objects
	 *            - other relevant objects. Currently in use is the {@link ClassFactory} instance to use for searching
	 *            for classes.
	 * @return <code>true</code> if the item is found / can be loaded.
	 */
	static boolean loadCheck(String path, SearchItemType searchType, Object... objects)
	{
		ClassFactory factory;
		File file;
		switch(searchType) {
		case CLASS:
			factory = (objects.length > 0 && objects[0] != null && objects[0] instanceof ClassFactory) ? (ClassFactory) objects[0] : PlatformUtils.getClassFactory();
			return factory.canLoadClass(path);
		case FILE:
			try {
				file = new File(path);
			} catch(Exception e) {
				return false;
			}
			return file.isFile();
		default:
			return false;
		}
	}
	
	/**
	 * Reunites the functionality of {@link #autoFind(ClassFactory, List, String, String, String, String, List)} and
	 * {@link #autoFind(ClassFactory, List, String, String, String, String, List)}.
	 * 
	 * @param given_packages
	 * @param given_cp
	 * @param upper_name
	 * @param lower_name
	 * @param entity
	 * @param checkedPaths
	 * @param searchType
	 * @param others
	 * @return the found path.
	 */
	static String autoFind(List<String> given_packages, String given_cp, String upper_name, String lower_name,
			String entity, List<String> checkedPaths, SearchItemType searchType, Object... others) {
		// delimiter between elements in the package hierarchy
		String D = searchType == SearchItemType.CLASS ? "." : "/";
		List<String> paths = checkedPaths != null ? checkedPaths : new LinkedList<>();
		paths.clear();
		paths.add(given_cp);
		if(given_cp != null && loadCheck(given_cp, searchType, others))
			return given_cp;
		List<String> packages = null;
		if(given_packages != null)
			if(searchType == SearchItemType.CLASS)
				packages = new ArrayList<>(given_packages);
			else {
				packages = new ArrayList<>(given_packages.size());
				for(String pack : given_packages)
					packages.add(pack.replace('.', '/'));
			}
		if(packages != null)
			for(String p : packages)
			{
				paths.add(p + D + given_cp);
				if(loadCheck(p + D + given_cp, searchType, others))
					return p + D + given_cp;
				if(searchType == SearchItemType.FILE)
					for(String dir : DeploymentConfiguration.SOURCE_FILE_DIRECTORIES)
					{
						paths.add(dir + D + p + D + given_cp);
						if(loadCheck(dir + D + p + D + given_cp, searchType, others))
							return dir + D + p + D + given_cp;
					}
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
		if(packages != null)
			roots.addAll(packages);
		roots.add(DeploymentConfiguration.ROOT_PACKAGE);
		roots.add(DeploymentConfiguration.CORE_PACKAGE);
		for(String r : roots)
			for(String cls : clsNames) {
				String upper_package = toPackageName(upper_name);
				String lower_package = lower_name != null ? toPackageName(lower_name) : null;
				paths.add(r + D + upper_package + D + cls);
				if(lower_name != null) {
					paths.add(r + D + upper_package + D + lower_package + D + cls);
					paths.add(r + D + lower_package + D + upper_package + D + cls);
					paths.add(r + D + lower_package + D + cls);
				}
				paths.add(r + D + cls);
			}
		for(String p : paths)
			if(loadCheck(p, searchType, others))
				return p;
		return null;
	}
	
	/**
	 * Makes the first letter of the given string upper-case.
	 * 
	 * @param s
	 *              - the string.
	 * @return the string with the first letter converted to upper-case.
	 */
	static String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	/**
	 * Attempts to create a package name out of a name.
	 * <p>
	 * If the name is all-upper-case, it lower-cases it; otherwise, it lower-cases only the first letter.
	 * 
	 * @param s
	 *            - the given string.
	 * @return the attempted package name.
	 */
	static String toPackageName(String s) {
		if(s.equals(s.toUpperCase())) // name is all-caps
			return s.toLowerCase();
		if(!Character.isUpperCase(s.charAt(0))) // name starts with lower case -> leave it like that
			return s;
		Matcher m = Pattern.compile("[A-Z]+").matcher(s);
		m.find(); // must match because tested before
		int nCaps = m.group().length() - 1;
		return s.substring(0, nCaps).toLowerCase() + s.substring(nCaps);
	}
}
