package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;

/**
 * RecorderService - Singleton Facade for the logging and monitoring subsystem.
 */
public class RecorderService {

    protected static volatile RecorderInterface	backend;
    private static volatile long				currentTick	= -1L;

    static {
        initBackend();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (backend != null)
                backend.shutdown();
        }));
    }

    private static void initBackend() {
        boolean enabled = Boolean.parseBoolean(System.getProperty("flash.recorder.enabled", "true"));
        boolean fileMode = Boolean.parseBoolean(System.getProperty("flash.recorder.file_mode", "true"));

        if (!enabled) {
            backend = new NullRecorder();
            System.out.println("[RECORDER] Subsystem is DISABLED.");
        } else if (fileMode) {
            backend = new FileRecorder();
            System.out.println("[RECORDER] Active Backend: FileRecorder (Writing to local JSON disk)");
        } else {
            backend = new WebRecorder();
        }
    }

    private RecorderService() {
    }

    public static void setCurrentTick(long tick) {
        currentTick = tick;
    }

    public static long getCurrentTick() {
        return currentTick;
    }

    /**
     * Flushes and closes the active recorder backend cleanly between sequential benchmark runs.
     */
    public static synchronized void shutdown() {
        if (backend != null) {
            backend.shutdown();
            backend = new NullRecorder();
        }
    }

    /**
     * Re-initializes the recorder backend according to current System properties.
     */
    public static synchronized void reinitialize() {
        shutdown();
        initBackend();
    }

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