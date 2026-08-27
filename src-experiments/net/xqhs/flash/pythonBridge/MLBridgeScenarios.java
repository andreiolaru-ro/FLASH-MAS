package net.xqhs.flash.pythonBridge;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.util.logging.Unit;

public class MLBridgeScenarios extends Unit {

    private static final String IRIS_FEATURES = "[5.1, 3.5, 1.4, 0.2]";

    private static final String MLP_FEATURES;

    static {
        // Build a simple 100-feature test vector
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            sb.append(i % 10 == 0 ? 1.0 : 0.5);
            if (i < 99) sb.append(", ");
        }
        sb.append("]");
        MLP_FEATURES = sb.toString();
    }

    private final PythonBridgeDriver driver;

    public MLBridgeScenarios(PythonBridgeDriver driver) {
        setUnitName("MLBridgeScenarios");
        this.driver = driver;
    }

    
     // Main entry point. Starts the bridge driver, runs all scenarios, then stops

    public static void main(String[] args) throws InterruptedException {
        PythonBridgeDriver driver = new PythonBridgeDriver();

        System.out.println("Starting Python bridge server");
        boolean started = driver.start();
        if (!started) {
            System.err.println("ERROR: Failed to start PythonBridgeDriver. Aborting.");
            return;
        }

        MLBridgeScenarios scenarios = new MLBridgeScenarios(driver);

        scenarios.testFastTrainingAndInference();
        scenarios.testLongTraining();

        System.out.println("\nWaiting up to 90 seconds for all async operations to finish...");
        Thread.sleep(90_000);

        driver.stop();
        System.out.println("Done. Driver stopped.");
    }

    
    private void sendWave(String destination, Map<String, String> payload, WaveReceiver callback) {
        AgentWave wave = new AgentWave();
        wave.appendDestination(destination);

        if (payload != null) {
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                wave.add(entry.getKey(), entry.getValue());
            }
        }

        if (callback == null) {
            // Synchronous call - blocks until the server responds
            long t0 = System.currentTimeMillis();
            AgentWave reply = driver.process(wave);
            long elapsed = System.currentTimeMillis() - t0;
            li("[sync -> {}] Response in {}ms: {}", destination, elapsed, reply.getContent());
        } else {
            // Asynchronous call - returns immediately, callback fires when done
            long t0 = System.currentTimeMillis();
            driver.processAsync(wave, reply -> {
                long elapsed = System.currentTimeMillis() - t0;
                li("[async -> {}] Callback fired in {}ms: {}", destination, elapsed, reply.getContent());
                callback.receive(reply);
            });
            li("[async -> {}] Call returned immediately, processing in background.", destination);
        }
    }

    //Test scenarios

    //Scenario 1: Fast training 
    public void testFastTrainingAndInference() {
        li("\n Scenario 1: Fast Training and Inference");

        li("Training logistic_iris model...");
        Map<String, String> trainPayload = new HashMap<>();
        trainPayload.put("model_id", "logistic_iris");
        sendWave("train", trainPayload, null);

        li("Running inference calls...");
        String[] irisTestCases = {
            "[5.1, 3.5, 1.4, 0.2]",
            "[6.7, 3.0, 5.2, 2.3]",
            "[5.9, 3.0, 4.2, 1.5]"
        };
        String[] expectedClasses = {"setosa (0)", "virginica (2)", "versicolor (1)"};

        for (int i = 0; i < irisTestCases.length; i++) {
            Map<String, String> predictPayload = new HashMap<>();
            predictPayload.put("model_id", "logistic_iris");
            predictPayload.put("features", irisTestCases[i]);
            li("Inference {} - expected: {}", i + 1, expectedClasses[i]);
            sendWave("predict", predictPayload, null);
        }

        li("Scenario 1 complete");
    }

    
    //Scenario 2: Long training
    
    public void testLongTraining() {
        li("\n Scenario 2: Long Async");

        // Launch MLP training asynchronously
        li("Launching mlp_large training ASYNCHRONOUSLY...");
        Map<String, String> trainPayload = new HashMap<>();
        trainPayload.put("model_id", "mlp_large");

        sendWave("train", trainPayload, reply -> {
            li("[MLP Training Complete] Result: {}", reply.getContent());

            li("Running inference on mlp_large after training...");
            Map<String, String> predictPayload = new HashMap<>();
            predictPayload.put("model_id", "mlp_large");
            predictPayload.put("features", MLP_FEATURES);
            sendWave("predict", predictPayload, null);
            li("[MLP Inference Complete]");
        });

        li("MLP is training in background");

        Map<String, String> fastPredict1 = new HashMap<>();
        fastPredict1.put("model_id", "logistic_iris");
        fastPredict1.put("features", "[4.9, 3.0, 1.4, 0.2]");
        sendWave("predict", fastPredict1, null);

        Map<String, String> fastPredict2 = new HashMap<>();
        fastPredict2.put("model_id", "logistic_iris");
        fastPredict2.put("features", "[7.0, 3.2, 4.7, 1.4]");
        sendWave("predict", fastPredict2, null);

        li("Fast inferences done");
        li("Scenario 2 complete");
    }
}
