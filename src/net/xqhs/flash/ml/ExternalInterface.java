package net.xqhs.flash.ml;

import java.util.Map;

/**
 * Abstract interface for communicating with external systems (e.g., Python).
 * Can be implemented using various technologies: HTTP, gRPC, Message Queues, JEP.
 */
public interface ExternalInterface {

    /**
     * Initialize the interface with configuration.
     *
     * @param config Configuration map
     * @return true if initialization successful
     */
    boolean initialize(Map<String, Object> config);

    /**
     * Send a request asynchronously to the external system.
     *
     * @param request Request data (format depends on implementation)
     * @param callback Callback for response or error
     */
    void sendAsync(String request, ResponseCallback callback);

    /**
     * Check if the interface is ready for communication.
     *
     * @return true if ready
     */
    boolean isReady();

    /**
     * Shutdown and cleanup resources.
     */
    void shutdown();

    /**
     * Callback interface for async responses.
     */
    interface ResponseCallback {
        /**
         * Called when response is received successfully.
         */
        void onResponse(String response);

        /**
         * Called when an error occurs.
         */
        void onError(Exception error);
    }
}