package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;

public interface RecorderInterface {
    void record(String entityName, AgentEvent event);
    void record(String entityName, String eventType, Object... args);
    void shutdown();
}