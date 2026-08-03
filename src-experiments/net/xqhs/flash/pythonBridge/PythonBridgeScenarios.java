package net.xqhs.flash.pythonBridge;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.util.logging.Unit;

public class PythonBridgeScenarios extends Unit {

    public PythonBridgeScenarios() {
        setUnitName("PythonBridgeScenarios");
    }

    public static void main(String[] args) throws InterruptedException {
        PythonBridgeScenarios scenarios = new PythonBridgeScenarios();
        scenarios.runAll();
    }

    public void runAll() throws InterruptedException {
        li("Starting mocked PYTHON-BRIDGE scenarios");

        MockAsyncDriver driver = new MockAsyncDriver();
        driver.configure(new MultiTreeMap().addSingleValue(
                DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "MockAsyncDriver"));
        
        li("Starting MockAsyncDriver...");
        driver.start();

        testQuickSync(driver);
        testSlowSync(driver);
        testSlowAsync(driver);
        testSubscribeProgress(driver);
        testCommonCallback(driver);

        li("\nWaiting for all async operations to finish...");
        Thread.sleep(8000);

        li("\nStopping MockAsyncDriver...");
        driver.stop();
        li("Mocked PYTHON-BRIDGE scenarios completed");
    }

    private void sendWave(MockAsyncDriver driver, String destination, Map<String, String> payload, WaveReceiver callback) {
        AgentWave wave = new AgentWave();
        wave.appendDestination(destination);
        
        if (payload != null) {
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                wave.add(entry.getKey(), entry.getValue());
            }
        }
        
        if (callback == null) {
            long start = System.currentTimeMillis();
            AgentWave reply = driver.process(wave);
            long end = System.currentTimeMillis();
            li("Sync Response: " + reply.getContent() + " (Time taken: " + (end - start) + " ms)");
        } else {
            long start = System.currentTimeMillis();
            driver.processAsync(wave, callback);
            long end = System.currentTimeMillis();
            li("Async call returned immediately in: " + (end - start) + " ms");
        }
    }

    public void testQuickSync(MockAsyncDriver driver) {
        li("\n Scenario 1: Quick Sync Call");
        Map<String, String> payload = new HashMap<>();
        payload.put("payload", "hello_world");
        sendWave(driver, "quickSync", payload, null);
    }

    public void testSlowSync(MockAsyncDriver driver) {
        li("\n Scenario 2: Slow Sync Call");
        Map<String, String> payload = new HashMap<>();
        payload.put("payload", "heavy_computation");
        payload.put("delayMs", "1500");
        sendWave(driver, "slowSync", payload, null);
    }

    public void testSlowAsync(MockAsyncDriver driver) {
        li("\n Scenario 3: Slow Async Call (Non-blocking)");
        Map<String, String> payload = new HashMap<>();
        payload.put("payload", "async_image_processing");
        payload.put("delayMs", "2000");
        
        long start = System.currentTimeMillis();
        sendWave(driver, "slowAsync", payload, reply -> {
            li("[Callback Scenario 3] Received async result: " + reply.getContent());
            li("[Callback Scenario 3] Time since request: " + (System.currentTimeMillis() - start) + " ms");
        });
    }
    
    public void testSubscribeProgress(MockAsyncDriver driver) {
        li("\n Scenario 4: Subscribe to Progress Notifications");
        Map<String, String> payload = new HashMap<>();
        payload.put("taskName", "ModelTrainingTask");

        sendWave(driver, "subscribeProgress", payload, reply -> {
            String status = reply.get("status");
            String progress = reply.get("progress");
            li("[Callback Scenario 4] [" + status + "] progress: " + progress + "% -> " + reply.getContent());
        });
    }

    public void testCommonCallback(MockAsyncDriver driver) {
        li("\n Scenario 5: Post several processings, same callback for all");
        
        WaveReceiver sharedCallback = reply -> {
            String taskName = reply.get("taskName");
            li("[Shared Callback] Received result for task '" + taskName + "': " + reply.getContent());
        };

        li("Method A: Submitting tasks sharing the same callback instance directly...");
        Map<String, String> payloadA1 = new HashMap<>();
        payloadA1.put("payload", "Sub-task A1");
        payloadA1.put("delayMs", "1200");
        payloadA1.put("taskName", "Task_A1");
        
        Map<String, String> payloadA2 = new HashMap<>();
        payloadA2.put("payload", "Sub-task A2");
        payloadA2.put("delayMs", "800");
        payloadA2.put("taskName", "Task_A2");

        sendWave(driver, "slowAsync", payloadA1, reply -> {
            reply.add("taskName", "Task_A1");
            sharedCallback.receive(reply);
        });
        
        sendWave(driver, "slowAsync", payloadA2, reply -> {
            reply.add("taskName", "Task_A2");
            sharedCallback.receive(reply);
        });

        li("Method B: Submitting tasks using a registered common callback session...");
        sendWave(driver, "registerCommonCallback", null, sharedCallback);

        Map<String, String> payloadB1 = new HashMap<>();
        payloadB1.put("taskName", "Registered_Task_B1");
        
        Map<String, String> payloadB2 = new HashMap<>();
        payloadB2.put("taskName", "Registered_Task_B2");

        sendWave(driver, "processCommon", payloadB1, reply -> {});
        sendWave(driver, "processCommon", payloadB2, reply -> {});
    }
}
