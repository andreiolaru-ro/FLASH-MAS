package automatedTesting;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import net.xqhs.flash.FlashBoot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigurableHelloWorldAgentTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    protected static final String       AGENT = "-agent AdvancedHelloWorldAgent classpath:example.agentConfiguration.ConfigurableHelloWorldAgent";
    protected static final String       STOP_AFTER_MS = " stopAfterMs:";
    protected static final int		DEFAULT_STOP_AFTER = 2000;
    protected static final int      ZERO = 0;
    protected static final int      ONE = 1;
    protected static final int      ONE_THOUSAND = 1000;
    protected static final int      FIVE_THOUSAND = 5000;

    @Before
    public void setUpStreams() {
        outContent.reset();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testExecution_whenWithoutParameter() throws Exception {
        checkCases(AGENT, DEFAULT_STOP_AFTER);
    }

    @Test
    public void testExecution_whenWithParameterZero() throws Exception {
        String configuration = AGENT + STOP_AFTER_MS + ZERO;
        checkCases(configuration, ZERO);
    }

    @Test
    public void testExecution_whenWithParameterOne() throws Exception {
        String configuration = AGENT + STOP_AFTER_MS + ONE;
        checkCases(configuration, ONE);
    }

    @Test
    public void testExecution_whenWithParameterOneThousand() throws Exception {
        String configuration = AGENT + STOP_AFTER_MS + ONE_THOUSAND;
        checkCases(configuration, ONE_THOUSAND);
    }

    @Test
    public void testExecution_whenWithParameterFiveThousand() throws Exception {
        String configuration = AGENT + STOP_AFTER_MS + FIVE_THOUSAND;
        checkCases(configuration, FIVE_THOUSAND);
    }

    private void checkCases(String configuration, int stopTime) throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(configuration.split(" ")));
        th.start();

        String consoleOutput;
        if (stopTime < 1000) {
            Thread.sleep(stopTime + 1000);
        }
        else {
            Thread.sleep(stopTime - 1000);

            consoleOutput = outContent.toString();

            assertFalse("Agent should not have stopped yet", consoleOutput.contains("[AdvancedHelloWorldAgent] stopped"));

            Thread.sleep(2000);
        }

        consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("[AdvancedHelloWorldAgent] starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + stopTime + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("[AdvancedHelloWorldAgent] stopped"));
    }

}