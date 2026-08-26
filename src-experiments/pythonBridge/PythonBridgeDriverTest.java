package pythonBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.pythonBridge.PythonBridgeDriver;
import net.xqhs.flash.pythonBridge.PythonHttpBridgeDriver;
import net.xqhs.flash.pythonBridge.PythonJepBridgeDriver;
import net.xqhs.flash.pythonBridge.PythonMqBridgeDriver;


public class PythonBridgeDriverTest {

    public static void main(String[] args) throws Exception {
        String transport = "mq";
        String method = "processAsync";

        String target = args.length > 0 ? args[0] : null;

        PythonBridgeDriver driver;

        switch(transport) {
            case "http":
                driver = new PythonHttpBridgeDriver();
                break;
            case "jep":
                driver = new PythonJepBridgeDriver();
                break;
            case "mq":
                driver = new PythonMqBridgeDriver();
                break;
            default:
                System.err.println("Unknown transport '" + transport + "'. Use one of: http, jep, mq");
                return;
        }

        if(!driver.start()) {
            System.err.println("Failed to start the Python bridge (" + transport + ").");
            return;
        }

        try {
            switch(method) {
                case "process": {
                    AgentWave request = request(transport, target, "hello via process()");
                    AgentWave reply = driver.process(request);
                    System.out.println("Sync reply: " + reply.getContent());
                    break;
                }
                case "processAsync": {
                    CountDownLatch done = new CountDownLatch(1);
                    AgentWave request = request(transport, target, "hello via processAsync()");
                    driver.processAsync(request, reply -> {
                        System.out.println("Async reply: " + reply.getContent());
                        done.countDown();
                    });
                    System.out.println("processAsync() returned; waiting for the callback...");
                    done.await(150, TimeUnit.SECONDS);
                    break;
                }
                case "batch": {
                    // several jobs posted at once, all sharing one callback
                    int jobs = 3;
                    CountDownLatch done = new CountDownLatch(jobs);
                    for(int i = 1; i <= jobs; i++) {
                        AgentWave request = request(transport, target, "batch job #" + i);
                        driver.processAsync(request, reply -> {
                            System.out.println("Batch reply: " + reply.getContent());
                            done.countDown();
                        });
                    }
                    System.out.println("All " + jobs + " jobs posted; waiting for callbacks...");
                    done.await(150, TimeUnit.SECONDS);
                    break;
                }
                case "get": {
                    if(!(driver instanceof PythonHttpBridgeDriver)) {
                        System.err.println("'get' is only available for the http transport.");
                        break;
                    }
                    System.out.println("GET /list reply: " + ((PythonHttpBridgeDriver) driver).get("list"));
                    break;
                }
                case "post": {
                    if(!(driver instanceof PythonHttpBridgeDriver)) {
                        System.err.println("'post' is only available for the http transport.");
                        break;
                    }
                    Map<String, String> params = new HashMap<>();
                    params.put("content", "hello via post()");
                    System.out.println(
                            "POST /call reply: " + ((PythonHttpBridgeDriver) driver).post("call", params));
                    break;
                }
                default:
                    System.err.println("Unknown method '" + method
                            + "'. Use one of: process, processAsync, batch, get, post");
            }
        } finally {
            driver.stop();
        }
    }

    /**
     * The destination element names what to call on the Python side, and means the same thing everywhere: an
     * endpoint for http, a function for jep. mq has a single job type, so it carries no destination.
     */
    protected static AgentWave request(String transport, String target, String content) {
        AgentWave wave = new AgentWave(content);
        if(target != null)
            wave.appendDestination(target);
        else if("http".equals(transport))
            wave.appendDestination("call");
        else if("jep".equals(transport))
            wave.appendDestination("run_inference");
        return wave;
    }
}