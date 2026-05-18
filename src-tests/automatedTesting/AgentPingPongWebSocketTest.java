package automatedTesting;

import net.xqhs.flash.FlashBoot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the WebSocket deployment scenario using composite agents.
 * This test verifies the correct instantiation of the WebSocket server, client connections,
 * agent registration over the network, and the integrity of the message exchange sequence.
 */
public class AgentPingPongWebSocketTest {

    private static final String DEPLOYMENT_ARGS = "-package testing -loader agent:composite " +
            "-node node1 -pylon webSocket:pylon1 serverPort:8886 " +
            "-agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentB -shard EchoTesting " +
            "-node node2 -pylon webSocket:pylon2 connectTo:ws://localhost:8886 " +
            "-agent composite:AgentB -shard messaging -shard PingBackTest -shard EchoTesting";

    private static final Pattern SERVER_STARTED_PATTERN = Pattern.compile("Server started successfully.");
    private static final Pattern CLIENT_CONNECTED_PATTERN = Pattern.compile("connected to \\[ws://localhost:8886]");

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.out.println("WARNING: WebSocket tests take longer to complete due to network/server startup. Please wait...");
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    private int countOccurrences(Pattern pattern, String consoleOutput) {
        Matcher matcher = pattern.matcher(consoleOutput);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @Test
    public void testExecution() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(DEPLOYMENT_ARGS.split(" ")));
        th.start();

        // Polling wait mechanism to accommodate WebSocket server startup and network latency
        // We wait up to 25 seconds for the nodes to shut down completely.
        int maxWaitCycles = 250;
        String consoleOutput = "";
        boolean isFinished = false;

        while (maxWaitCycles > 0) {
            Thread.sleep(100);
            consoleOutput = outContent.toString();
            // The deployment ends successfully when both nodes stop
            if (consoleOutput.contains("Node [node1] stopped.") && consoleOutput.contains("Node [node2] stopped.")) {
                isFinished = true;
                break;
            }
            maxWaitCycles--;
        }

        assertTrue("Test timed out before nodes stopped gracefully. Output: \n" + consoleOutput, isFinished);

        // 1. Verify WebSocket Infrastructure
        assertEquals("WebSocket server should start exactly once.", 1, countOccurrences(SERVER_STARTED_PATTERN, consoleOutput));
        // We expect 2 connections (one from pylon1 locally, one from pylon2 remotely)
        assertEquals("Both pylons should connect to the server.", 2, countOccurrences(CLIENT_CONNECTED_PATTERN, consoleOutput));

        // 2. Verify Directory Registration
        assertTrue("AgentA should be registered on node1", consoleOutput.contains("Registered entity [AgentA] on [node1]"));
        assertTrue("AgentB should be registered on node2", consoleOutput.contains("Registered entity [AgentB] on [node2]"));

        // 3. Check order of ping and pong messages
        for (int i = 1; i < 4; i++) {
            int currentPing = consoleOutput.lastIndexOf("[ping-no " + i + "]");
            int currentPong = consoleOutput.lastIndexOf("[ping-no " + i + " reply]");
            int nextPing = consoleOutput.lastIndexOf("[ping-no " + (i + 1) + "]");
            assertTrue("Wrong sequence of events", currentPing < currentPong);
            assertTrue("Wrong sequence of events", currentPong < nextPing);
        }

        // 4. Verify Final State and Disconnection
        assertTrue("AgentA should send the last ping.", consoleOutput.contains("ping-last 5"));
        assertTrue("AgentA should stop after the limit.", consoleOutput.contains("[AgentA] stopped"));
        assertTrue("AgentB should stop after the limit.", consoleOutput.contains("[AgentB] stopped"));

        // 5. Verify Pylons unregistered the entities
        assertTrue("Pylon1 should unregister AgentA.", consoleOutput.contains("\"unregister\":\"unregister\"}"));
        assertTrue("Pylon2 should unregister AgentB.", consoleOutput.contains("\"entityName\":\"AgentB\",\"unregister\":\"unregister\"}"));
    }
}