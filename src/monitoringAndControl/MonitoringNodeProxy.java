package monitoringAndControl;

/*
 * This interface should be implemented by any proxy to a {@link Node} that offers monitoring services.
 */

public interface MonitoringNodeProxy extends NodeProxy {

    /**
     * Registers an entity with the specified name, associating with it a {@link MonitoringReceiver} instance.
     *
     * @param entityName
     *                      - the name of the entity.
     * @param receiver
     *                      - the {@link MonitoringReceiver} instance to receive command.
     * @return an indication of success.
     */
    boolean register(String entityName, MonitoringReceiver receiver);

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
