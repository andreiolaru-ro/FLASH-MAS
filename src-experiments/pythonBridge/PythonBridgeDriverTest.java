package pythonBridge;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.pythonBridge.PythonBridgeDriver;

import java.util.HashMap;
import java.util.Map;


public class PythonBridgeDriverTest {

    public static void main(String[] args) throws InterruptedException {
        String method = "post";

        PythonBridgeDriver driver = new PythonBridgeDriver();

        if(!driver.start()) {
            System.err.println("Failed to start the Python bridge.");
            return;
        }

        switch(method) {
            case "process": {
                AgentWave request = new AgentWave("hello from the Java side (via process())");
                request.appendDestination("call");
                AgentWave reply = driver.process(request);
                System.out.println("Sync reply: " + reply.getContent());
                break;
            }
            case "processAsync": {
                AgentWave request = new AgentWave("hello from the Java side (via processAsync())");
                request.appendDestination("call");
                driver.processAsync(request, reply -> System.out.println("Async reply: " + reply.getContent()));
                Thread.sleep(3000);
                break;
            }
            case "get": {
                try {
                    String response = driver.get("list");
                    System.out.println("GET /list reply: " + response);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            }
            case "post": {
                Map<String, String> params = new HashMap<>();
                params.put("content", "hello from the Java side (via post())");
                try {
                    String response = driver.post("call", params);
                    System.out.println("POST /call reply: " + response);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            }
            default:
                System.err.println("Unknown method '" + method + "'. Use one of: process, processAsync, get, post");
        }

        driver.stop();
    }
}