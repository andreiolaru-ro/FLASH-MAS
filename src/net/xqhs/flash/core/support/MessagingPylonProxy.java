package net.xqhs.flash.core.support;

/**
 * This interface should be implemented by any proxy to a {@link Pylon} that offers messaging services.
 * 
 * @author Vlad Tălmaciu, Andrei Olaru
 */
public interface MessagingPylonProxy extends PylonProxy
{
	/**
	 * Registers an agent with the specified name, associating with it a {@link MessageReceiver} instance.
	 * 
	 * @param agentName
	 *                      - the name of the agent.
	 * @param receiver
	 *                      - the {@link MessageReceiver} instance to receive messages.
	 * @return an indication of success.
	 */
	boolean register(String agentName, MessageReceiver receiver);
	
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

	/**
	 * Register the local node.
	 *
	 * @param nodeName
	 *                        - the name of node.
	 * @param inbox
	 * 						  - the receiver of the registered node
	 */
	void registerNode(String nodeName, MessageReceiver inbox);

	/**
	 * Register the entity for monitoring and control within the context of local pylon.
	 * @param name
	 * 				- the name of node.
	 * @param inbox
	 * 				- the receiver of the registered entity
	 */
	void registerCentralEntity(String name, MessageReceiver inbox);

	/**
	 * Unregisters an agent with the specified name and associated {@link MessageReceiver} instance.
	 *
	 * @param agentName
	 * 						- the name of the agent
	 * @return
	 * 						- an indication of success.
	 */
	boolean unregister(String agentName);
}

