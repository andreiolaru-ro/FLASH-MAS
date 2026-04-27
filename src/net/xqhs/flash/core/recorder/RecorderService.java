package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;

/**
 * RecorderService - Singleton Facade for the logging and monitoring subsystem.
 * <p>
 * Decouples agent logic from the storage mechanism. Reads System Properties
 * initialized by the WebBridge to dynamically determine if events should be
 * saved to a local JSON file or streamed live to the Web UI.
 * </p>
 */
public class RecorderService {

    protected static final RecorderInterface backend;

    static {
        // Read configuration flags injected by the hybrid controller
        boolean enabled = Boolean.parseBoolean(System.getProperty("flash.recorder.enabled", "true"));
        boolean fileMode = Boolean.parseBoolean(System.getProperty("flash.recorder.file_mode", "true"));

        if (!enabled) {
            backend = new NullRecorder();
            System.out.println("[RECORDER] Subsystem is DISABLED.");
        } else if (fileMode) {
            backend = new FileRecorder();
            System.out.println("[RECORDER] Active Backend: FileRecorder (Writing to local JSON disk)");
        } else {
            // Stream directly to the Single Page Application UI
            backend = new WebRecorder();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (backend != null) backend.shutdown();
        }));
    }

    private RecorderService() {}

    public static void record(String entityName, AgentEvent event) {
        backend.record(entityName, event);
    }

    public static void record(String entityName, String eventType, Object... args) {
        backend.record(entityName, eventType, args);
    }

    public static void record(String agent, String source, String dest, String content) {
        backend.record(agent, source, dest, content);
    }

    public static void record(String agent, AgentWave wave, String eventType) {
        backend.record(agent, wave, eventType);
    }
}