package pythonBridge;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.pythonBridge.PythonBridgeDriver;


public class PythonBridgeDriverTest {

    public static void main(String[] args) throws InterruptedException {
        PythonBridgeDriver driver = new PythonBridgeDriver();

        if(!driver.start()) {
            System.err.println("Failed to start the Python bridge.");
            return;
        }

        AgentWave request = new AgentWave("hello from the Java side");
        request.appendDestination("call");

        driver.processAsync(request, reply -> System.out.println("Got async reply: " + reply.getContent()));

        Thread.sleep(3000);
        driver.stop();
    }
}