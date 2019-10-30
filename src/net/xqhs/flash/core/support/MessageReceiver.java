package net.xqhs.flash.core.support;

/**
 * This interface should be implemented by any entity which is able to receive messages from a support infrastructure
 * (and, more concretely, from a Pylon).
 * <p>
 * In practice, instances of this interface should allow the link between a {@link Pylon} and a {@link MessagingShard},
 * without exposing to the Pylon a reference to the shard.
 * 
 * @author Andrei Olaru
 */
public interface MessageReceiver {
	/**
	 * The method to be called when a message is received.
	 * 
	 * @param source
	 *                        - the source of the message.
	 * @param destination
	 *                        - the destination of the message.
	 * @param content
	 *                        - the content of the message.
	 */
	public void receive(String source, String destination, String content);
}
