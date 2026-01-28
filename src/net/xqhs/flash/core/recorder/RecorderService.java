package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;

/**
 * Singleton Facade for the Recording subsystem.
 * Determines at startup whether to use a real FileRecorder or a dummy NullRecorder.
 * JIT compiler should completely ignore the dummy, leading to zero overhead when ENABLED is set to false.
 */
public class RecorderService {
    public static final boolean ENABLED = true;
    protected static final RecorderInterface backend;

    static {
        if (ENABLED) {
            backend = new FileRecorder();
        } else {
            backend = new NullRecorder();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(backend::shutdown));
    }

    private RecorderService() {}

    /**
     * Public Accessor - Methods delegate to the active backend
     */
    public static void record(String entityName, AgentEvent event) {
        backend.record(entityName, event);
    }

    public static void record(String entityName, String eventType, Object... args) {
        backend.record(entityName, eventType, args);
    }

    public static RecorderInterface getInstance() {
        return backend;
    }
}