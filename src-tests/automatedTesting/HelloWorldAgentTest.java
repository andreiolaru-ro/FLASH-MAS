package automatedTesting;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import example.helloWorld.BootDeployment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HelloWorldAgentTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testExecution() throws InterruptedException {
        Thread bootThread = new Thread(() -> BootDeployment.main(new String[]{}));
        bootThread.start();

        for (int i = 0; i < 10; i++) {
            if (outContent.toString().contains("Agent stopped")) {
                break; // stop waiting if we already see the message
            }
            Thread.sleep(500); // check every 500ms
        }

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("Agent started"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World"));
        assertTrue("Agent stop message not found", consoleOutput.contains("Agent stopped"));
    }
}