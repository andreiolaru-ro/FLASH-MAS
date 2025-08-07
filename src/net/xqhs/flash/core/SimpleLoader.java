package net.xqhs.flash.core;

import java.util.List;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * Simple implementation for {@link Loader}, which attempts to load the entity class by instantiating it:
 * <ul>
 * <li>using a constructor that receives a {@link MultiTreeMap} as argument, or
 * <li>using the no-argument constructor, and if the instance implements {@link ConfigurableEntity}, subsequently
 * calling {@link ConfigurableEntity#configure}.
 * </ul>
 * <p>
 * The class is loaded using the classpath provided as the first value in the simple key of the given entity
 * configuration. If no such classpath exists, loading fails.
 * 
 * @author andreiolaru
 */
public class SimpleLoader implements Loader<Entity<?>> {
	/**
	 * Name of the key in which the classpath should be stored.
	 */
	public static final String	CLASSPATH_KEY	= "classpath";
	/**
	 * The {@link LoadPack} instance to use.
	 */
	protected LoadPack			lp;
	
	/**
	 * The configuration is not used.
	 */
	@Override
	public boolean configure(MultiTreeMap configuration, LoadPack loadPack) {
		lp = loadPack;
		return true;
	}
	
	/**
	 * Context is not considered in any way.
	 */
	@Override
	public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
		return preload(configuration);
	}
	
	/**
	 * The preload method tests if the classpath is set and exists.
	 */
	@Override
	public boolean preload(MultiTreeMap configuration) {
		if(!configuration.isSimple(CLASSPATH_KEY)) {
			lp.le("Classpath not set in configuration (missing classpath key)");
			return false;
		}
		if(!PlatformUtils.getClassFactory().canLoadClass(configuration.get(CLASSPATH_KEY))) {
			lp.le("Class cannot be loaded from given classpath []", configuration.get(CLASSPATH_KEY));
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
	 * <li>using the no-argument constructor, and if the instance implements {@link ConfigurableEntity}, the
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
			List<MultiTreeMap> subordinateEntities) {
		if(preload(configuration)) {
			String classpath = configuration.get(CLASSPATH_KEY);
			Entity<?> loaded = null;
			try {
				// try Constructor(configuration)
				loaded = (Entity<?>) lp.getClassFactory().loadClassInstance(classpath, configuration, false);
			} catch(Exception e) {
				if(e instanceof NoSuchMethodException) {// no constructor with configuration argument
					try {
						loaded = (Entity<?>) lp.getClassFactory().loadClassInstance(classpath, null, true);
						if(loaded instanceof ConfigurableEntity) {// no-argument constructor used, must try to configure
							if(!((ConfigurableEntity<?>) loaded).configure(configuration))
								lp.le("Entity [] loaded from [] but configuration failed.", loaded.getName(),
										classpath);
						}
						else
							lp.le("Configuration not sent to entity [] loaded from []", loaded.getName(), classpath);
					} catch(Exception e1) {
						lp.le("Failed to load class [] via no-argument constructor: ", classpath,
								PlatformUtils.printException(e));
					}
				}
				else
					lp.le("Failed to load class [] via constructor with configuration: ",
							configuration.get(CLASSPATH_KEY), PlatformUtils.printException(e));
			}
			if(loaded != null && context != null)
				for(EntityProxy<? extends Entity<?>> c : context)
					loaded.addGeneralContext(c);
			return loaded;
		}
		return null;
	}
	
	@Override
	public Entity<?> load(MultiTreeMap configuration) {
		return load(configuration, null, null);
	}
}