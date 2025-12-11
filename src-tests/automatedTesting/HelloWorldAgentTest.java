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
		Thread th = new Thread(() -> FlashBoot.main(HELLO_WORLD_CONFIG.split(" ")));
        th.start();

        Thread.sleep(2000);

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
		int startingMessage = consoleOutput.indexOf("[HelloWorldAgent] starting");
		int helloWorldMessage = consoleOutput.indexOf("Hello World");
		int stoppedMessage = consoleOutput.indexOf("[HelloWorldAgent] stopped");
		assertTrue("Wrong sequence of events", startingMessage < helloWorldMessage);
		assertTrue("Wrong sequence of events", helloWorldMessage < stoppedMessage);
    }
}