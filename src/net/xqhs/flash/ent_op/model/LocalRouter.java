package net.xqhs.flash.ent_op.model;

import net.xqhs.flash.core.util.MultiTreeMap;

public interface LocalRouter extends EntityAPI {

	/**
	 * Configures the instance and connects it to the {@link FMas} instance.
	 * 
	 * <b>NOTE</b> that when linking to the {@link FMas}, this may be called with a <code>null</code> configuration, but
	 * this should not invalidate the previously setup configuration.
	 * 
	 * @param configuration
	 *            - the configuration. May be <code>null</code> if only the link with the {@link FMas} should be set up
	 *            by this call.
	 * @param fmas
	 *            - the {@link FMas} instance.
	 * @return <code>true</code> if the setup is successful.
	 */
	boolean setup(MultiTreeMap configuration, FMas fmas);
	
	/**
	 * Routes a wave based on the target entity.
	 *
	 * @param wave
	 *            - the wave that must be routed.
	 */
    void route(Wave wave);
}
