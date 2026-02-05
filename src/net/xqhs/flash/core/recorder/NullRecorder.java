package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;

public class NullRecorder implements RecorderInterface {
    @Override
    public void record(String entityName, AgentEvent event) {
    }

    @Override
    public void record(String entityName, String eventType, Object... args) {
    }

    @Override
    public void record(String agent, String source, String dest, String content) {
    }

    @Override
    public void record(String agent, AgentWave wave, String eventType) {
    }

    @Override
    public void shutdown() {
    }
}