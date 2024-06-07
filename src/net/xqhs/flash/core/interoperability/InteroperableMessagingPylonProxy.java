package net.xqhs.flash.core.interoperability;

import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;

public interface InteroperableMessagingPylonProxy extends WaveMessagingPylonProxy {

	/**
	 * The key in the JSON object which is assigned to the name of the bridge entity (for bridge registration messages).
	 */
	public final String	BRIDGE_KEY			= "platformPrefix";

	/**
	 * The separator between the platform prefix and the port.
	 */
	public final String	PLATFORM_PREFIX_SEPARATOR	= "(?<!/)/(?!/)";

	/**
	 * The key used for the wsRegions protocol for registering bridge only as a bridge and not as a normal entity.
	 */
	public final String	REMOTE_BRIDGE					= "isRemote";

	/**
	 * Registers a bridge entity with the specified name, associating with it a {@link WaveReceiver} instance
	 * and an identifier corresponding to the support infrastructure the bridge entity can route messages to.
	 * @param platformPrefix
	 *              - the identifier for the support infrastructure
	 * @param waveReceiver
	 *              - the {@link WaveReceiver} instance to receive messages
	 * @return an indication of success.
	 */
	boolean registerBridge(String platformPrefix, WaveReceiver waveReceiver);

	/**
	 * @return the identifier for the support infrastructure.
	 */
	String getPlatformPrefix();
}
