package net.xqhs.flash.core.support;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.shard.AgentShardDesignation;

/**
 * {@link EntityProxy} for a {@link Pylon}, specifying the only one required method, for retrieving the recommended
 * implementation for shards connecting to the pylon.
 * 
 * @author Andrei Olaru
 */
public interface PylonProxy extends EntityProxy<Pylon>
{
	/**
	 * Retrieves the name of the class for an agent shard implementation that is recommended by this support
	 * infrastructure, for the specified shard type, if any. If no such recommendation exists, <code>null</code> will be
	 * returned.
	 * <p>
	 * 
	 * @see Pylon
	 * 
	 * @param shardType
	 *                      - the type/name of the shard to be recommended.
	 * @return the name of the class containing the recommended implementation, or <code>null</code> if no
	 *         recommendation is made.
	 */
	public String getRecommendedShardImplementation(AgentShardDesignation shardType);
}
