package net.xqhs.flash.core.support;

/**
 * This interface should be implemented by any proxy to a {@link Pylon} that offers messaging services.
 * 
 * @author Vlad Tălmaciu, Andrei Olaru
 */
public interface MessagingPylonProxy extends PylonProxy
{
	/**
	 * Registers an entity with the specified name, associating with it a {@link MessageReceiver} instance.
	 * 
	 * @param entityName
	 *                      - the name of the entity
	 * @param receiver
	 *                      - the {@link MessageReceiver} instance to receive messages.
	 * @return an indication of success.
	 */
	boolean register(String entityName, MessageReceiver receiver);
	
	/**
	 * Requests to the pylon to send a message.
	 * 
	 * @param source
	 *                        - the source endpoint.
	 * @param destination
	 *                        - the destination endpoint.
	 * @param content
	 *                        - the content of the message.
	 * @return an indication of success.
	 */
	boolean send(String source, String destination, String content);
}

