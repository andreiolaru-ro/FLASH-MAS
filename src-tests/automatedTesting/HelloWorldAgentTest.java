package automatedTesting;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.xqhs.flash.FlashBoot;

public class HelloWorldAgentTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
	protected static final String		HELLO_WORLD_CONFIG	= "-agent HelloWorldAgent classpath:example.helloWorld.HelloWorldAgent";

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
		Thread bootThread = new Thread(() -> FlashBoot.main(HELLO_WORLD_CONFIG.split(" ")));
        bootThread.start();

        for (int i = 0; i < 10; i++) {
            if (outContent.toString().contains("stopped")) {
                break; // stop waiting if we already see the message
            }
            Thread.sleep(500); // check every 500ms
        }

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
		int a = consoleOutput.indexOf("starting");
		int b = consoleOutput.indexOf("Hello World");
		int c = consoleOutput.indexOf("stopped");
		assertTrue("Wrong sequence of events", a < b);
		assertTrue("Wrong sequence of events", c < b);
    }
}