package net.xqhs.flash.core.recorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import net.xqhs.flash.core.agent.AgentEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileRecorder implements RecorderInterface {
    protected ConcurrentLinkedQueue<SimulationEvent> eventBuffer;
    protected AtomicBoolean isRunning;
    protected Gson gson;
    protected JsonWriter jsonWriter;
    protected FileWriter fileWriter;

    public FileRecorder() {
        this.eventBuffer = new ConcurrentLinkedQueue<>();
        this.isRunning = new AtomicBoolean(true);
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "simulation_log_" + timestamp + ".json";

            this.fileWriter = new FileWriter(fileName);
            this.jsonWriter = new JsonWriter(fileWriter);
            this.jsonWriter.setIndent("  ");
            this.jsonWriter.beginArray();

            System.out.println("[RECORDER] Started logging to: " + fileName);

        } catch (IOException e) {
            System.err.println("[RECORDER] Failed to initialize file writer: " + e.getMessage());
            e.printStackTrace();
        }

        Thread writerThread = new Thread(this::processQueue, "Recorder-IO");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    @Override
    public void record(String entityName, AgentEvent event) {
        if (jsonWriter != null) {
            String type = (event != null && event.getType() != null) ? event.getType().toString() : "UNKNOWN_EVENT";
            push(new SimulationEvent(entityName, type, event));
        }
    }

    @Override
    public void record(String entityName, String eventType, Object... args) {
        if (jsonWriter != null) {
            push(new SimulationEvent(entityName, eventType, args));
        }
    }

    private void push(SimulationEvent event) {
        if (isRunning.get()) eventBuffer.offer(event);
    }

    private void processQueue() {
        while (isRunning.get() || !eventBuffer.isEmpty()) {
            SimulationEvent event = eventBuffer.poll();
            if (event != null) {
                try {
                    synchronized (this) {
                        if (jsonWriter != null) {
                            gson.toJson(event, SimulationEvent.class, jsonWriter);
                            jsonWriter.flush();
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[RECORDER] Write error: " + e.getMessage());
                }
            } else {
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    @Override
    public void shutdown() {
        System.out.println("[RECORDER] Finalizing log file...");
        isRunning.set(false);
        synchronized (this) {
            try {
                if (jsonWriter != null) {
                    while(!eventBuffer.isEmpty()) {
                        SimulationEvent evt = eventBuffer.poll();
                        if(evt != null) gson.toJson(evt, SimulationEvent.class, jsonWriter);
                    }
                    jsonWriter.endArray();
                    jsonWriter.close();
                    fileWriter.close();
                    System.out.println("[RECORDER] Log file closed successfully.");
                }
            } catch (IOException e) {
                System.err.println("[RECORDER] Error closing log file: " + e.getMessage());
            }
        }
    }
}