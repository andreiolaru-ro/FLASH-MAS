package net.xqhs.flash.core.support;

/**
 * This interface should be used as proxy by pylons which represent messages as source/destination/content triples.
 */
public interface ClassicMessagingPylonProxy extends MessagingPylonProxy {
	/**
	 * Registers an entity with the specified name, associating with it a {@link ClassicMessageReceiver} instance.
	 * 
	 * @param entityName
	 *            - the name of the entity
	 * @param receiver
	 *            - the {@link ClassicMessageReceiver} instance to receive messages.
	 * @return an indication of success.
	 */
	boolean register(String entityName, ClassicMessageReceiver receiver);
	
	/**
	 * Unregistered the entity with the specified name, de-associating it from this pylon.
	 * <p>
	 * It is good practice that pylons enforce the verification that the calling entity indeed has a reference to the
	 * previously registered message receiver.
	 * 
	 * @param entityName
	 *            - the name of the entity.
	 * @param registeredReceiver
	 *            - the {@link ClassicMessageReceiver} previously associated with the entity.
	 * @return an indication of success (e.g. return <code>false</code> if the entity had not been registered).
	 */
	boolean unregister(String entityName, ClassicMessageReceiver registeredReceiver);
	
	/**
	 * Requests to the pylon to send a message.
	 * 
	 * @param source
	 *            - the source endpoint.
	 * @param destination
	 *            - the destination endpoint.
	 * @param content
	 *            - the content of the message.
	 * @return an indication of success.
	 */
	boolean send(String source, String destination, String content);
}
