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

import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.logging.Logger;

/**
 * A loader instance has the capability of creating new {@link Entity} instances.
 * <p>
 * Entities are created based on a configuration that is loaded at boot time and is represented as a
 * {@link TreeParameterSet}.
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
	public boolean configure(TreeParameterSet configuration, Logger log);
	
	/**
	 * Performs checks and completes the configuration.
	 * <p>
	 * This method <i>may</i> be implemented by implementing classes in order to
	 * <ul>
	 * <li>Check that an {@link Entity} of type <code>T</code> and with the given configuration can indeed be loaded; it
	 * this method returns <code>true</code>, then calling {@link #load(TreeParameterSet)} with the same configuration
	 * is expected to complete successfully.
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
	public boolean preload(TreeParameterSet configuration);
	
	/**
	 * Loads a new instance of entity <b>T</b>.
	 * 
	 * @param configuration
	 *            - the configuration data for the entity.
	 * @return the entity, if loading has been successful.
	 */
	public T load(TreeParameterSet configuration);
	
	/**
	 * Simple implementation for {@link Loader}, which attempts to load the entity class by instantiating it:
	 * <ul>
	 * <li>using a constructor that receives a {@link TreeParameterSet} as argument, or
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
		public boolean configure(TreeParameterSet configuration, Logger _log)
		{
			log = _log;
			return true;
		}
		
		/**
		 * The preload method tests if the classpath is set and exists.
		 */
		@Override
		public boolean preload(TreeParameterSet configuration)
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
		 * <li>using a constructor that receives a {@link TreeParameterSet} as argument, passing the given
		 * configuration;
		 * <li>using the default constructor, and if the instance implements {@link ConfigurableEntity}, the
		 * {@link ConfigurableEntity#configure} method will be called with the given configuration.
		 * </ul>
		 */
		@Override
		public Entity<?> load(TreeParameterSet configuration)
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
							return loaded;
						} catch(Exception e1)
						{
							if(log != null)
								log.le("Failed to load class [] via default constructor:", classpath,
										PlatformUtils.printException(e));
						}
					}
					if(log != null)
						log.le("Failed to load class [] via constructor with configuration:",
								configuration.get(CLASSPATH_KEY), PlatformUtils.printException(e));
				}
			}
			return null;
		}
	}
}
