package net.xqhs.flash.core;

import net.xqhs.flash.core.util.TreeParameterSet;

/**
 * A loader instance has the capability of creating new {@link Entity} instances.
 * 
 * @param <T>
 *            the type of {@link Entity} instance that the loader can load.
 * 
 * @author andreiolaru
 */
public interface Loader<T extends Entity<?>>
{
	/**
	 * Loads a new instance of entity <b>T</b>.
	 * 
	 * @param configuration
	 *            - the configuration data for the entity.
	 * @return the entity, if loading has been successful.
	 */
	public T load(TreeParameterSet configuration);
}
