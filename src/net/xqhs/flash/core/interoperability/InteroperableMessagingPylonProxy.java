package net.xqhs.flash.core.interoperability;

import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;

public interface InteroperableMessagingPylonProxy extends WaveMessagingPylonProxy {

	/**
	 * Registers a bridge entity with the specified name, associating with it a {@link MessageReceiver} instance and an identifier corresponding to 
	 * the support infrastructure the bridge entity can route messages to.
	 * 
	 * @param entityName
	 *                      - the name of the bridge entity
	 * @param platformPrefix
	 *                      - the identifier for the support infrastructure
	 * @param receiver
	 *                      - the {@link WaveReceiver} instance to receive messages.
	 * @return an indication of success.
	 */
	boolean registerBridge(String entityName, String platformPrefix, WaveReceiver receiver);
}
