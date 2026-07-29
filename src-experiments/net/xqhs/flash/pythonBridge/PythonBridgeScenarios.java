package net.xqhs.flash.pythonBridge;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.support.WaveReceiver;

public class PythonBridgeScenarios {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting mocked PYTHON-BRIDGE cenarios");

        MockAsyncDriver driver = new MockAsyncDriver();
        driver.configure(new MultiTreeMap().addSingleValue(
                DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "MockAsyncDriver"));
        
        System.out.println("Starting MockAsyncDriver...");
        driver.start();

        System.out.println("\n Scenario 1: Quick Sync Call");
        AgentWave wave1 = new AgentWave();
        wave1.add("action", "quickSync");
        wave1.add("payload", "hello_world");
        
        long start1 = System.currentTimeMillis();
        AgentWave reply1 = driver.process(wave1);
        long end1 = System.currentTimeMillis();
        System.out.println("Quick Sync Response: " + reply1.getContent());
        System.out.println("Time taken: " + (end1 - start1) + " ms");

        System.out.println("\n Scenario 2: Slow Sync Call");
        AgentWave wave2 = new AgentWave();
        wave2.add("action", "slowSync");
        wave2.add("payload", "heavy_computation");
        wave2.add("delayMs", "1500");
        
        long start2 = System.currentTimeMillis();
        AgentWave reply2 = driver.process(wave2);
        long end2 = System.currentTimeMillis();
        System.out.println("Slow Sync Response: " + reply2.getContent());
        System.out.println("Time taken: " + (end2 - start2) + " ms");

        System.out.println("\n Scenario 3: Slow Async Call (Non-blocking) ");
        AgentWave wave3 = new AgentWave();
        wave3.add("action", "slowAsync");
        wave3.add("payload", "async_image_processing");
        wave3.add("delayMs", "2000");

        long start3 = System.currentTimeMillis();
        driver.processAsync(wave3, reply -> {
            System.out.println("[Callback Scenario 3] Received async result: " + reply.getContent());
            System.out.println("[Callback Scenario 3] Time since request: " + (System.currentTimeMillis() - start3) + " ms");
        });
        long end3 = System.currentTimeMillis();
        System.out.println("Returned from processAsync immediately in: " + (end3 - start3) + " ms");

        System.out.println("\n Scenario 4: Subscribe to Progress Notifications ");
        AgentWave wave4 = new AgentWave();
        wave4.add("action", "subscribeProgress");
        wave4.add("taskName", "ModelTrainingTask");

        driver.processAsync(wave4, reply -> {
            String status = reply.get("status");
            String progress = reply.get("progress");
            System.out.println("[Callback Scenario 4] [" + status + "] progress: " + progress + "% -> " + reply.getContent());
        });
        System.out.println("Subscribed and returned from processAsync instantly.");

        System.out.println("\n Scenario 5: Post several processings, same callback for all");
        
        WaveReceiver sharedCallback = reply -> {
            String taskName = reply.get("taskName");
            System.out.println("Received result for task '" + taskName + "': " + reply.getContent());
        };

        System.out.println("Method A: Submitting tasks sharing the same callback instance directly");
        AgentWave task1 = new AgentWave();
        task1.add("action", "slowAsync");
        task1.add("payload", "Sub-task A1");
        task1.add("delayMs", "1200");
        task1.add("taskName", "Task_A1");
        
        AgentWave task2 = new AgentWave();
        task2.add("action", "slowAsync");
        task2.add("payload", "Sub-task A2");
        task2.add("delayMs", "800");
        task2.add("taskName", "Task_A2");

        driver.processAsync(task1, reply -> {
            reply.add("taskName", "Task_A1");
            sharedCallback.receive(reply);
        });
        driver.processAsync(task2, reply -> {
            reply.add("taskName", "Task_A2");
            sharedCallback.receive(reply);
        });

        System.out.println("Method B: Submitting tasks using a registered common callback session");
        
        AgentWave registerWave = new AgentWave();
        registerWave.add("action", "registerCommonCallback");
        driver.processAsync(registerWave, sharedCallback);

        AgentWave processWave1 = new AgentWave();
        processWave1.add("action", "processCommon");
        processWave1.add("taskName", "Registered_Task_B1");

        AgentWave processWave2 = new AgentWave();
        processWave2.add("action", "processCommon");
        processWave2.add("taskName", "Registered_Task_B2");

        driver.processAsync(processWave1, null);
        driver.processAsync(processWave2, null);

        System.out.println("\nWaiting for all async operations to finish");
        Thread.sleep(8000);

        System.out.println("\nStopping MockAsyncDriver...");
        driver.stop();
        System.out.println("Mocked PYTHON-BRIDGE scenarios completed");
    }
}
