package automatedTesting;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import example.agentConfiguration.BootDeployment;
import net.xqhs.flash.FlashBoot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ConfigurableHelloWorldAgentTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    protected static final Long		DEFAULT_STOP_AFTER			= 2000L;
    protected static final Long     ZERO = 0L;
    protected static final Long     ONE = 1L;
    protected static final Long     ONE_THOUSAND = 1000L;
    protected static final Long     FIVE_THOUSAND = 5000L;
    protected static final String		CONFIGURABLE_HELLO_WORLD_CONFIG_WITHOUT_PARAMETER	= "-agent AdvancedHelloWorldAgent classpath:example.agentConfiguration.ConfigurableHelloWorldAgent";
    protected static final String		CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_ZERO	= "-agent AdvancedHelloWorldAgent classpath:example.agentConfiguration.ConfigurableHelloWorldAgent stopAfterMs:0";
    protected static final String		CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_ONE	= "-agent AdvancedHelloWorldAgent classpath:example.agentConfiguration.ConfigurableHelloWorldAgent stopAfterMs:1";
    protected static final String		CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_ONE_THOUSAND	= "-agent AdvancedHelloWorldAgent classpath:example.agentConfiguration.ConfigurableHelloWorldAgent stopAfterMs:1000";
    protected static final String		CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_FIVE_THOUSAND	= "-agent AdvancedHelloWorldAgent classpath:example.agentConfiguration.ConfigurableHelloWorldAgent stopAfterMs:5000";

    @Before
    public void setUpStreams() {
        outContent.reset();
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
    public void testExecution_whenUsingConfigurationFromExample() throws Exception {
        long stopAfterMs = readStopAfterMsFromDeployment();

        Thread th = new Thread(() -> BootDeployment.main(new String[]{}));
        th.start();

        Thread.sleep(stopAfterMs + 1000);

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + stopAfterMs + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
    }

    @Test
    public void testExecution_whenUsingConfigurationFromExample_thenAgentNotStoppingTooEarly() throws Exception {
        long stopAfterMs = readStopAfterMsFromDeployment();

        Thread th = new Thread(() -> BootDeployment.main(new String[]{}));
        th.start();

        // Sleep for less than stopAfterMs
        Thread.sleep(stopAfterMs / 3);

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + stopAfterMs + " ms)"));
        assertFalse("Agent should not have stopped yet", consoleOutput.contains("stopped"));
    }

    @Test
    public void testExecution_whenWithoutParameter() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(CONFIGURABLE_HELLO_WORLD_CONFIG_WITHOUT_PARAMETER.split(" ")));
        th.start();

        // Sleep for less than stopAfterMs
        Thread.sleep(DEFAULT_STOP_AFTER - 1000);

        String consoleOutput = outContent.toString();

        assertFalse("Agent should not have stopped yet", consoleOutput.contains("stopped"));

        Thread.sleep(2000);

        consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + DEFAULT_STOP_AFTER + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
    }

    @Test
    public void testExecution_whenWithParameterZero() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_ZERO.split(" ")));
        th.start();

        Thread.sleep(ZERO + 1000);

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + ZERO + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
    }

    @Test
    public void testExecution_whenWithParameterOne() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_ONE.split(" ")));
        th.start();

        Thread.sleep(ONE + 1000);

        String consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + ONE + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
    }

    @Test
    public void testExecution_whenWithParameterOneThousand() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_ONE_THOUSAND.split(" ")));
        th.start();

        // Sleep for less than stopAfterMs
        Thread.sleep(ONE_THOUSAND - 1000);

        String consoleOutput = outContent.toString();

        assertFalse("Agent should not have stopped yet", consoleOutput.contains("stopped"));

        Thread.sleep(2000);

        consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + ONE_THOUSAND + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
    }

    @Test
    public void testExecution_whenWithParameterFiveThousand() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(CONFIGURABLE_HELLO_WORLD_CONFIG_WITH_PARAMETER_FIVE_THOUSAND.split(" ")));
        th.start();

        // Sleep for less than stopAfterMs
        Thread.sleep(FIVE_THOUSAND - 1000);

        String consoleOutput = outContent.toString();

        assertFalse("Agent should not have stopped yet", consoleOutput.contains("stopped"));

        Thread.sleep(2000);

        consoleOutput = outContent.toString();

        assertTrue("Agent start message not found", consoleOutput.contains("starting"));
        assertTrue("Hello World message not found", consoleOutput.contains("Hello World (stopping in " + FIVE_THOUSAND + " ms)"));
        assertTrue("Agent stop message not found", consoleOutput.contains("stopped"));
    }

}