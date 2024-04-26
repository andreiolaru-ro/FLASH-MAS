package net.xqhs.flash.core.support;

import net.xqhs.flash.core.agent.AgentWave;

/**
 * This interface should be used as proxy by pylons which are able to work directly with {@link AgentWave}s, without the
 * need to transform them to a source/destination/content triple.
 */
public interface WaveMessagingPylonProxy extends PylonProxy {
	/**
	 * Registers an entity with the specified name, associating with it a {@link MessageReceiver} instance.
	 * 
	 * @param entityName
	 *            - the name of the entity
	 * @param receiver
	 *            - the {@link MessageReceiver} instance to receive messages.
	 * @return an indication of success.
	 */
	boolean register(String entityName, WaveReceiver receiver);
	
	/**
	 * Unregistered the entity with the specified name, de-associating it from this pylon.
	 * <p>
	 * It is good practice that pylons enforce the verification that the calling entity indeed has a reference to the
	 * previously registered message receiver.
	 * 
	 * @param entityName
	 *            - the name of the entity.
	 * @param registeredReceiver
	 *            - the {@link MessageReceiver} previously associated with the entity.
	 * @return an indication of success (e.g. return <code>false</code> if the entity had not been registered).
	 */
	boolean unregister(String entityName, WaveReceiver registeredReceiver);
	
	/**
	 * Requests to the pylon to send a message.
	 * 
	 * @param wave
	 *            - the wave to send.
	 * @return an indication of success.
	 */
	boolean send(AgentWave wave);
}
