package net.xqhs.flash.core.support;

public interface InteroperableMessagingPylonProxy extends MessagingPylonProxy {

	/**
	 * Registers a bridge entity with the specified name, associating with it a {@link MessageReceiver} instance and an identifier corresponding to 
	 * the support infrastructure the bridge entity can route messages to.
	 * 
	 * @param entityName
	 *                      - the name of the bridge entity
	 * @param platformPrefix
	 *                      - the identifier for the support infrastructure
	 * @param receiver
	 *                      - the {@link MessageReceiver} instance to receive messages.
	 * @return an indication of success.
	 */
	boolean registerBridge(String entityName, String platformPrefix, MessageReceiver receiver);
}
