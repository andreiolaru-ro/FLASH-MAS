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
package net.xqhs.flash.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Logger;

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
 *            the type of {@link Entity} instance that the loader can load.
 * 
 * @author andreiolaru
 */
public interface Loader<T extends Entity<?>>
{
	/**
	 * Configures an instance of the loader based on deployment data.
	 * 
	 * @param configuration
	 *            - the deployment data.
	 * @param log
	 *            - a {@link Logger} instance to use for logging messages, during the loader's activity.
	 * @return <code>true</code> if the configuration process was successful and the {@link Loader} instance is ready to
	 *         load entities; <code>false</code> if this instance cannot be expected to work normally.
	 */
	public boolean configure(MultiTreeMap configuration, Logger log);
	
	/**
	 * Same as {@link #preload(MultiTreeMap)}, but performs the checks in the given context.
	 * 
	 * @param configuration
	 *            - the configuration data for the entity.
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
	 *            - the configuration of the entity that one intends to load. This COnfiguration may be modified (added
	 *            to) in this method.
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
	 *            - the configuration data for the entity.
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
	 *            - the configuration data for the entity.
	 * @return the entity, if loading has been successful.
	 */
	public T load(MultiTreeMap configuration);
	
	/**
	 * Simple implementation for {@link Loader}, which attempts to load the entity class by instantiating it:
	 * <ul>
	 * <li>using a constructor that receives a {@link MultiTreeMap} as argument, or
	 * <li>using the default constructor, and if the instance implements {@link ConfigurableEntity}, subsequently
	 * calling {@link ConfigurableEntity#configure}.
	 * </ul>
	 * <p>
	 * The class is loaded using the classpath provided as the first value in the simple key of the given entity
	 * configuration. If no such classpath exists, loading fails.
	 * 
	 * @author andreiolaru
	 */
	public class SimpleLoader implements Loader<Entity<?>>
	{
		/**
		 * Name of the key in which the classpath should be stored.
		 */
		public static final String	CLASSPATH_KEY	= "classpath";
		
		/**
		 * The log to use.
		 */
		protected Logger			log				= null;
		
		@Override
		public boolean configure(MultiTreeMap configuration, Logger _log)
		{
			log = _log;
			return true;
		}
		
		/**
		 * Context is not considered in any way.
		 */
		@Override
		public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context)
		{
			return preload(configuration);
		}
		
		/**
		 * The preload method tests if the classpath is set and exists.
		 */
		@Override
		public boolean preload(MultiTreeMap configuration)
		{
			if(!configuration.isSimple(CLASSPATH_KEY))
			{
				if(log != null)
					log.le("Classpath not set in configuration (missing classpath key)");
				return false;
			}
			if(!PlatformUtils.getClassFactory().canLoadClass(configuration.get(CLASSPATH_KEY)))
			{
				if(log != null)
					log.le("Class cannot be loaded from given classpath []", configuration.get(CLASSPATH_KEY));
				return false;
			}
			return true;
		}
		
		/**
		 * The method will attempt to instantiate a new entity, using the classpath from the {@value #CLASSPATH_KEY}
		 * parameter in the configuration (as a simple key).
		 * <p>
		 * The instantiation is tried in order:
		 * <ul>
		 * <li>using a constructor that receives a {@link MultiTreeMap} as argument, passing the given configuration;
		 * <li>using the default constructor, and if the instance implements {@link ConfigurableEntity}, the
		 * {@link ConfigurableEntity#configure} method will be called with the given configuration.
		 * </ul>
		 * <p>
		 * After loading, all entities in the <code>context</code> parameter are added as context, using
		 * {@link Entity#addGeneralContext(EntityProxy)}.
		 * <p>
		 * No subordinate entities will be loaded.
		 * 
		 * @param subordinateEntities
		 *            - this will not be used.
		 */
		@Override
		public Entity<?> load(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context,
				List<MultiTreeMap> subordinateEntities)
		{
			if(preload(configuration))
			{
				String classpath = configuration.get(CLASSPATH_KEY);
				try
				{
					// try Constructor(configuration)
					return (Entity<?>) PlatformUtils.getClassFactory().loadClassInstance(classpath, configuration,
							false);
				} catch(Exception e)
				{
					if(e instanceof NoSuchMethodException)
					{// no constructor with configuration argument
						try
						{
							Entity<?> loaded = (Entity<?>) PlatformUtils.getClassFactory().loadClassInstance(classpath,
									null, true);
							if(loaded instanceof ConfigurableEntity)
							{// default constructor used, must try to configure
								if(!((ConfigurableEntity<?>) loaded).configure(configuration) && log != null)
									log.le("Entity [] loaded from [] but configuration failed.", loaded.getName(),
											classpath);
							}
							else
								log.le("Configuration not sent to entity [] loaded from []", loaded.getName(),
										classpath);
							if(context != null)
								for(EntityProxy<? extends Entity<?>> c : context)
									loaded.addGeneralContext(c);
							return loaded;
						} catch(Exception e1)
						{
							if(log != null)
								log.le("Failed to load class [] via default constructor: ", classpath,
										PlatformUtils.printException(e));
						}
					}
					if(log != null)
						log.le("Failed to load class [] via constructor with configuration: ",
								configuration.get(CLASSPATH_KEY), PlatformUtils.printException(e));
				}
			}
			return null;
		}
		
		@Override
		public Entity<?> load(MultiTreeMap configuration)
		{
			return load(configuration, null, null);
		}
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
	 *                         - the {@link ClassFactory} that can test if the class exists / can be loaded.
	 * @param packages
	 *                         - a list of java packages in which to search.
	 * @param given_cp
	 *                         - a classpath or a class name that may be given directly, saving the effort of searching
	 *                         for the class. This classpath will also be searched in the list of packages.
	 * @param upper_name
	 *                         - the upper name in the kind hierarchy of the entity (should not be <code>null</code>).
	 * @param lower_name
	 *                         - the upper name in the kind hierarchy of the entity (can be <code>null</code>).
	 * @param entity
	 *                         - the name of the entity for which a class is searched (should not be <code>null</code>).
	 * @param checkedPaths
	 *                         - a {@link List} in which all checked paths will be added (checked paths are classpaths
	 *                         where the class have been searched).
	 * @return the full classpath of the first class that has been found, if any; <code>null</code> otherwise.
	 */
	static String autoFind(ClassFactory factory, List<String> packages, String given_cp, String upper_name,
			String lower_name, String entity, List<String> checkedPaths)
	{
		String D = ".";
		List<String> paths = checkedPaths != null ? checkedPaths : new LinkedList<>();
		paths.clear();
		paths.add(given_cp);
		if(given_cp != null && factory.canLoadClass(given_cp))
			return given_cp;
		if(packages != null)
			for(String p : packages)
			{
				paths.add(p + D + given_cp);
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
		roots.add(DeploymentConfiguration.ROOT_PACKAGE);
		roots.add(DeploymentConfiguration.CORE_PACKAGE);
		roots.addAll(packages);
		for(String cls : clsNames)
			for(String r : roots)
			{
				paths.add(r + D + upper_name + D + cls);
				if(lower_name != null)
				{
					paths.add(r + D + upper_name + D + lower_name + D + cls);
					paths.add(r + D + lower_name + D + upper_name + D + cls);
					paths.add(r + D + lower_name + D + cls);
				}
			}
		for(String p : paths)
			if(factory.canLoadClass(p))
				return p;
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
}
