package net.xqhs.flash.core.recorder;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.tools.player.MultiAgentReplayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebRecorder - Asynchronous Event Transmitter.
 * <p>
 * Operates as a daemon thread to consume Flash-MAS events and push them
 * to the LiveEventStream without blocking the main agent execution logic.
 * </p>
 */
public class WebRecorder implements RecorderInterface, Runnable {

    private final ConcurrentLinkedQueue<SimulationEvent> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public WebRecorder() {
        System.out.println("[RECORDER] Active Backend: WebRecorder (Live UI Monitoring)");
        Thread daemon = new Thread(this, "WebRecorder-Daemon");
        daemon.setDaemon(true);
        daemon.start();
    }

    @Override
    public void record(String entityName, AgentEvent event) {
        if (entityName == null) return;
        String type = (event != null && event.getType() != null) ? event.getType().toString() : "EVENT";
        queue.add(new SimulationEvent(entityName, type, event.toString()));
    }

    @Override
    public void record(String entityName, String eventType, Object... args) {
        if (entityName == null) return;
        queue.add(new SimulationEvent(entityName, eventType, args));
    }

    @Override
    public void record(String agent, String source, String dest, String content) {
        if (agent == null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source", source);
        data.put("destination", dest);
        data.put("content", content);
        queue.add(new SimulationEvent(agent, "MESSAGE_STRING", data));
    }

    @Override
    public void record(String agent, AgentWave wave, String eventType) {
        if (agent == null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("waveId", wave.hashCode());
        data.put("content", wave.getSerializedContent());
        queue.add(new SimulationEvent(agent, eventType, data));
    }

    @Override
    public void run() {
        // Continuously drain the queue and forward to the Replayer's bridge
        while (isRunning.get() || !queue.isEmpty()) {
            SimulationEvent ev = queue.poll();
            if (ev != null) {
                MultiAgentReplayer.LiveEventStream.add(ev);
            } else {
                try { Thread.sleep(20); } catch (InterruptedException e) { break; }
            }
        }
    }

    @Override
    public void shutdown() {
        isRunning.set(false);
    }
}