package monitoringAndControl;

/**
 * This interface should be implemented by any entity which is able to receive monitoring and control commands
 * from a support infrastructure (and, more concretely, from a Pylon).
 */
public interface MonitoringReceiver {
    /**
     * The method to be called when a command is received.
     *
     * @param source
     *                        - the source of the message.
     * @param destination
     *                        - the destination of the message.
     * @param command
     *                        - the content of the message.
     */
    public void receive(String source, String destination, String command);
}

