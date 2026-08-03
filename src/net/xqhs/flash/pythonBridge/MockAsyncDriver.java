package net.xqhs.flash.pythonBridge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.ml.AsyncDriver;

public class MockAsyncDriver extends EntityCore<Node> implements EntityProxy<MockAsyncDriver>, AsyncDriver {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private WaveReceiver registeredCallback = null;

    public MockAsyncDriver() {
        super();
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }
        li("MockAsyncDriver started successfully.");
        return true;
    }

    @Override
    public boolean stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        li("MockAsyncDriver stopped.");
        return super.stop();
    }

    @Override
    public EntityProxy<MockAsyncDriver> asContext() {
        return this;
    }

    @Override
    public String getEntityName() {
        return getName();
    }

    private String getEndpoint(AgentWave wave) {
        String[] destinations = wave.getDestinationElements();
        return (destinations != null && destinations.length > 0) ? destinations[0] : "quickSync";
    }

    @Override
    public AgentWave process(AgentWave wave) {
        String endpoint = getEndpoint(wave);

        if ("quickSync".equals(endpoint)) {
            return handleQuickSync(wave);
        } else if ("slowSync".equals(endpoint)) {
            return handleSlowSync(wave);
        } else {
            AgentWave errorReply = wave.createReply("ERROR: Action '" + endpoint + "' is asynchronous and should be called via processAsync.");
            errorReply.add("status", "ERROR");
            return errorReply;
        }
    }

    @Override
    public void processAsync(AgentWave wave, WaveReceiver callback) {
        String endpoint = getEndpoint(wave);

        if ("quickSync".equals(endpoint)) {
            li("Processing quickSync asynchronously (invoking callback immediately)");
            AgentWave reply = handleQuickSync(wave);
            if (callback != null) {
                callback.receive(reply);
            }
        } else if ("slowSync".equals(endpoint)) {
            li("Processing slowSync asynchronously (blocking current thread)");
            AgentWave reply = handleSlowSync(wave);
            if (callback != null) {
                callback.receive(reply);
            }
        } else if ("slowAsync".equals(endpoint)) {
            handleSlowAsync(wave, callback);
        } else if ("subscribeProgress".equals(endpoint)) {
            handleSubscribeProgress(wave, callback);
        } else if ("registerCommonCallback".equals(endpoint)) {
            handleRegisterCommonCallback(wave, callback);
        } else if ("processCommon".equals(endpoint)) {
            handleProcessCommon(wave, callback);
        } else {
            AgentWave errorReply = wave.createReply("ERROR: Unknown endpoint '" + endpoint + "'");
            errorReply.add("status", "ERROR");
            if (callback != null) {
                callback.receive(errorReply);
            }
        }
    }

    private AgentWave handleQuickSync(AgentWave wave) {
        li("Processing quickSync call synchronously");
        String payload = wave.get("payload");
        AgentWave reply = wave.createReply("Quick sync reply: processed '" + payload + "' successfully.");
        reply.add("status", "SUCCESS");
        return reply;
    }

    private AgentWave handleSlowSync(AgentWave wave) {
        li("Processing slowSync call synchronously (blocking)");
        String payload = wave.get("payload");
        long delayMs = 2000;
        String delayStr = wave.get("delayMs");
        if (delayStr != null) {
            try {
                delayMs = Long.parseLong(delayStr);
            } catch (NumberFormatException e) {
                }
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AgentWave reply = wave.createReply("Slow sync reply: processed '" + payload + "' after " + delayMs + "ms sleep.");
        reply.add("status", "SUCCESS");
        return reply;
    }

    private void handleSlowAsync(AgentWave wave, WaveReceiver callback) {
        li("Processing slowAsync: returning instantly, running action in background thread");
        String payload = wave.get("payload");
        long delayMs = 3000;
        String delayStr = wave.get("delayMs");
        if (delayStr != null) {
            try {
                delayMs = Long.parseLong(delayStr);
            } catch (NumberFormatException e) {
                }
        }
        final long finalDelay = delayMs;
        executor.submit(() -> {
            try {
                Thread.sleep(finalDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            AgentWave reply = wave.createReply("Slow async reply: processed '" + payload + "' in background after " + finalDelay + "ms.");
            reply.add("status", "SUCCESS");
            if (callback != null) {
                callback.receive(reply);
            }
        });
    }

    private void handleSubscribeProgress(AgentWave wave, WaveReceiver callback) {
        li("Processing subscribeProgress: subscribing callback to periodic progress updates");
        String taskName = wave.get("taskName");
        if (taskName == null) {
            taskName = "DefaultTask";
        }
        final String finalTaskName = taskName;
        executor.submit(() -> {
            int steps = 5;
            for (int i = 1; i <= steps; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                int percent = (i * 100) / steps;
                AgentWave progressUpdate = wave.createReply("Progress update for " + finalTaskName + ": " + percent + "%");
                progressUpdate.add("status", "RUNNING");
                progressUpdate.add("progress", String.valueOf(percent));
                progressUpdate.add("taskName", finalTaskName);
                if (callback != null) {
                    callback.receive(progressUpdate);
                }
            }
            AgentWave finalReply = wave.createReply("Task " + finalTaskName + " completed successfully.");
            finalReply.add("status", "SUCCESS");
            finalReply.add("progress", "100");
            finalReply.add("taskName", finalTaskName);
            if (callback != null) {
                callback.receive(finalReply);
            }
        });
    }

    private void handleRegisterCommonCallback(AgentWave wave, WaveReceiver callback) {
        li("Registering common callback for multi-processing");
        this.registeredCallback = callback;
        AgentWave reply = wave.createReply("Callback registered successfully.");
        reply.add("status", "SUCCESS");
        if (callback != null) {
            callback.receive(reply);
        }
    }

    private void handleProcessCommon(AgentWave wave, WaveReceiver callback) {
        li("Processing task for common callback");
        String taskName = wave.get("taskName");
        if (this.registeredCallback == null) {
            le("Error: No common callback registered!");
            AgentWave errorReply = wave.createReply("ERROR: No common callback registered.");
            errorReply.add("status", "ERROR");
            if (callback != null) {
                callback.receive(errorReply);
            }
            return;
        }
        executor.submit(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            AgentWave reply = wave.createReply("Common callback processing result for '" + taskName + "'.");
            reply.add("status", "SUCCESS");
            reply.add("taskName", taskName);
            this.registeredCallback.receive(reply);
        });
    }
}
