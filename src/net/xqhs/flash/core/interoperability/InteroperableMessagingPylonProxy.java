package net.xqhs.flash.core.interoperability;

import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;

/**
 * The interface all pylon proxies must implement for platforms that can be interoperated.
 */
public interface InteroperableMessagingPylonProxy extends WaveMessagingPylonProxy {

	/**
	 * The key in the JSON object which is assigned to the name of the bridge entity (for bridge registration messages).
	 */
	public final String	MESSAGE_BRIDGE_KEY			= "platformPrefix";

	/**
	 * The separator between the platform prefix and the port.
	 */
	public final String	PLATFORM_PREFIX_SEPARATOR	= "(?<!/)/(?!/)";

	/**
	 * The key used for the wsRegions protocol for registering bridge only as a bridge and not as a normal entity.
	 */
	public final String	IS_REMOTE					= "isRemote";

	/**
	 * Registers a bridge entity with the specified name, associating with it a {@link WaveReceiver} instance
	 * and an identifier corresponding to the support infrastructure the bridge entity can route messages to.
	 * 
	 * @param entityName
	 *              - the name of the bridge entity
	 * @param waveReceiver
	 *              - the {@link WaveReceiver} instance to receive messages
	 * @param platformPrefix
	 *              - the identifier for the support infrastructure
	 * @return an indication of success.
	 */
	boolean registerBridge(String entityName, WaveReceiver waveReceiver, String platformPrefix);

	/**
	 * @return the identifier for the support infrastructure.
	 */
	String getPlatformPrefix();
}
