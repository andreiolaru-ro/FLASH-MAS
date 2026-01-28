package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;

public class NullRecorder implements RecorderInterface {
    @Override
    public void record(String entityName, AgentEvent event) {
    }

    @Override
    public void record(String entityName, String eventType, Object... args) {
    }

    @Override
    public void shutdown() {
    }
}