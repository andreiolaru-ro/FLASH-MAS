package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;

public interface RecorderInterface {
    void record(String entityName, AgentEvent event);
    void record(String entityName, String eventType, Object... args);
    void record(String agent, String source, String dest, String content);
    void record(String agent, AgentWave wave, String eventType);
    void shutdown();
}