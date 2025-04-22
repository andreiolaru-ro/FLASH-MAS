package automatedTesting;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import example.agentConfiguration.BootDeployment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class AdvancedHelloWorldAgentTest {
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

    private long readStopAfterMsFromDeployment() throws Exception {
        File xmlFile = new File("src-examples/example/agentConfiguration/deployment.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        NodeList paramList = doc.getElementsByTagName("parameter");
        long stopAfterMs = 2000; // default

        for (int i = 0; i < paramList.getLength(); i++) {
            Element param = (Element) paramList.item(i);
            if ("stopAfterMs".equals(param.getAttribute("name"))) {
                stopAfterMs = Long.parseLong(param.getAttribute("value"));
                break;
            }
        }
        return stopAfterMs;
    }

    @Test
    public void testExecution() throws Exception {
        long stopAfterMs = readStopAfterMsFromDeployment();

        Thread bootThread = new Thread(() -> BootDeployment.main(new String[]{}));
        bootThread.start();

        Thread.sleep(stopAfterMs + 1000);

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("Agent started"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + stopAfterMs + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("Agent stopped"));
    }

    @Test
    public void testAgentNotStoppedTooEarly() throws Exception {
        long stopAfterMs = readStopAfterMsFromDeployment();

        Thread bootThread = new Thread(() -> BootDeployment.main(new String[]{}));
        bootThread.start();

        // Sleep for less than stopAfterMs
        Thread.sleep(Math.max(500, stopAfterMs / 2));

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("Agent started"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + stopAfterMs + " ms)"));
        assertFalse("Agent should not have stopped yet", consoleOutput.contains("Agent stopped"));
    }
}