/**
 * 
 */
package net.xqhs.flash.core.mobileComposite;

import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Interface for shards that cannot be serialized during the mobility process.
 * 
 * @author Andrei Olaru
 */
public interface NonSerializableShard extends AgentShard
{
	/**
	 * @return the configuration of the shard, which can be used to reconstruct the shard.
	 */
	MultiTreeMap getConfiguration();
}
