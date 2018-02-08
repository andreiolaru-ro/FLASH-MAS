package net.xqhs.flash.core.deployment;

import net.xqhs.flash.core.util.TreeParameterSet;

/**
 * A loader instance has the capability of creating new {@link Thing} instances.
 * 
 * @param <T>
 *            the type of {@link Thing} instance that the loader can load.
 * 
 * @author andreiolaru
 */
public interface Loader<T extends Thing<?>>
{
	/**
	 * Loads a new instance of thing <b>T</b>.
	 * 
	 * @param configuration
	 *            - the configuration data for the thing.
	 * @return the thing, if loading has been successful.
	 */
	public T load(TreeParameterSet configuration);
}
