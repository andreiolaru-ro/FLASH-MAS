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
 * Test class for the WebSocket deployment scenario focusing on Composite Agent Mobility.
 * This test verifies agent migration between nodes, shard serialization,
 * and the integrity of the message exchange sequence.
 */
public class AgentMobilityWebSocketTest {

    protected static final String WS_PORT = "8995";

    protected static final String PRELUDE = "-package testing test.compositeMobility -loader agent:composite -loader agent:mobileComposite ";

    protected static final String NODE_A = "-node nodeA -pylon webSocket:pylonA serverPort:" + WS_PORT + " ";

    protected static final String NODE_A_AGENTS = "-agent mobileComposite:agentA1 -shard messaging -shard EchoTesting -shard MobilityTest to:nodeB time:5000 " +
            "-shard PingTest every:500 n:10 otherAgent:agentB1 otherAgent:agentA2 " +
            "-agent agentA2 -shard messaging -shard EchoTesting -shard PingBackTest ";

    protected static final String NODE_B = "-node nodeB -pylon webSocket:pylonB connectTo:ws://localhost:" + WS_PORT + " ";

    protected static final String NODE_B_AGENTS = "-agent agentB1 -shard messaging -shard EchoTesting -shard PingBackTest";

    protected static final String DEPLOYMENT_ARGS = PRELUDE + NODE_A + NODE_A_AGENTS + NODE_B + NODE_B_AGENTS;

    private static final Pattern SERVER_STARTED_PATTERN = Pattern.compile("Server started successfully.");
    private static final Pattern AGENT_A1_MIGRATION_TO_B_PATTERN = Pattern.compile("preparing to move to \\[nodeB]");
    private static final Pattern AGENT_A1_MIGRATION_TO_A_PATTERN = Pattern.compile("preparing to move to \\[nodeA]");
    private static final Pattern AGENT_A1_SERIALIZATION_PATTERN = Pattern.compile("Serializing shards \\[\\{");
    private static final Pattern AGENT_B1_RECEIVED_PING_PATTERN = Pattern.compile("agent \\[agentB1] event: \\[\\{EVENT_TYPE=\\[AGENT_WAVE], content=\\[ping-(no \\d+|last \\d+)]");

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.out.println("WARNING: WebSocket mobility tests take longer to complete due to network/server startup and migration latency. Please wait...");
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

        // Polling wait mechanism to wait for the system to shut down
        int maxWaitCycles = 400;
        String consoleOutput = "";
        boolean isFinished = false;

        while (maxWaitCycles > 0) {
            Thread.sleep(100);
            consoleOutput = outContent.toString();
            // The simulation finishes when nodes stop
            if (consoleOutput.contains("Node [nodeA] stopped.") && consoleOutput.contains("Node [nodeB] stopped.")) {
                Thread.sleep(1000);
                consoleOutput = outContent.toString();
                isFinished = true;
                break;
            }
            maxWaitCycles--;
        }

        assertTrue("Test timed out before nodes stopped gracefully. Output: \n" + consoleOutput, isFinished);

        // 1. Verify WebSocket Infrastructure
        assertEquals("WebSocket server should start exactly once.", 1, countOccurrences(SERVER_STARTED_PATTERN, consoleOutput));

        // 2. Verify Migrations
        assertEquals("agentA1 should initiate migration to nodeB.", 1, countOccurrences(AGENT_A1_MIGRATION_TO_B_PATTERN, consoleOutput));
        assertEquals("agentA1 should initiate return migration to nodeA.", 1, countOccurrences(AGENT_A1_MIGRATION_TO_A_PATTERN, consoleOutput));

        // Assert serialization occurs during the process
        assertTrue("agentA1 should serialize its shards during migration.", countOccurrences(AGENT_A1_SERIALIZATION_PATTERN, consoleOutput) >= 2);

        // 3. Verify Message Exchange Integrity (Ping/Pong)
        int pingsReceivedByB1 = countOccurrences(AGENT_B1_RECEIVED_PING_PATTERN, consoleOutput);
        assertTrue("agentB1 should have received multiple pings during the lifecycle.", pingsReceivedByB1 > 5);

        // 4. Verify Final State
        assertTrue("agentA1 should stop after its tasks are complete.", consoleOutput.contains("[agentA1] stopped"));
        assertTrue("agentB1 should stop.", consoleOutput.contains("[agentB1] stopped"));
    }
}