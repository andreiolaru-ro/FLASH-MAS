package automatedTesting;

import example.simplePingPong.Boot;
import net.xqhs.flash.FlashBoot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import testing.AgentPingPongPlain;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgentPingPongTest {

    protected static final String		PING_PONG_CONFIG_ONE_MESSAGE_AGENT_PING_PONG_PLAIN = "-package testing -node main -agent AgentA classpath:AgentPingPongPlain sendTo:AgentB sendTo:AgentC ping-number:1 -agent AgentB classpath:AgentPingPongPlain -agent AgentC classpath:AgentPingPongPlain";

    protected static final String		PING_PONG_CONFIG_FIVE_MESSAGES_AGENT_PING_PONG_PLAIN = "-package testing -node main -agent AgentA classpath:AgentPingPongPlain sendTo:AgentB sendTo:AgentC ping-number:5 -agent AgentB classpath:AgentPingPongPlain -agent AgentC classpath:AgentPingPongPlain";

    protected static final String		PING_PONG_CONFIG_TWENTY_MESSAGES_AGENT_PING_PONG_PLAIN = "-package testing -node main -agent AgentA classpath:AgentPingPongPlain sendTo:AgentB sendTo:AgentC ping-number:20 -agent AgentB classpath:AgentPingPongPlain -agent AgentC classpath:AgentPingPongPlain";

    protected static final String		PING_PONG_CONFIG_ONE_MESSAGE_AGENT_PING_PONG = "-package testing -node main -agent AgentA classpath:AgentPingPong sendTo:AgentB sendTo:AgentC ping-number:1 -agent AgentB classpath:AgentPingPong -agent AgentC classpath:AgentPingPong";

    protected static final String		PING_PONG_CONFIG_FIVE_MESSAGES_AGENT_PING_PONG = "-package testing -node main -agent AgentA classpath:AgentPingPong sendTo:AgentB sendTo:AgentC ping-number:5 -agent AgentB classpath:AgentPingPong -agent AgentC classpath:AgentPingPong";

    protected static final String		PING_PONG_CONFIG_TWENTY_MESSAGES_AGENT_PING_PONG = "-package testing -node main -agent AgentA classpath:AgentPingPong sendTo:AgentB sendTo:AgentC ping-number:20 -agent AgentB classpath:AgentPingPong -agent AgentC classpath:AgentPingPong";

    private static final Pattern PING_NUMBER_PATTERN =
			Pattern.compile("\\[ping-number\\]>\\s*\\[(-?\\d+)\\]");

	private static final Pattern SEND_TO_PATTERN =
			Pattern.compile("\\[sendTo\\]>\\s*\\[([^\\]]*)\\]");

    private static final Pattern AGENT_A_PING_PATTERN = Pattern.compile("AgentA, ping");

    private static final Pattern AGENT_A_PONG_PATTERN = Pattern.compile("AgentA, pong");

    private static final Pattern AGENT_B_PONG_PATTERN = Pattern.compile("AgentB, pong");

    private static final Pattern AGENT_B_PING_PATTERN = Pattern.compile("AgentB, ping");

    private static final Pattern AGENT_C_PONG_PATTERN = Pattern.compile("AgentC, pong");

    private static final Pattern AGENT_C_PING_PATTERN = Pattern.compile("AgentC, ping");

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

	private Integer getDefaultPingNumber() throws NoSuchFieldException, IllegalAccessException {
			Field f = AgentPingPongPlain.class.getDeclaredField("DEFAULT_PING_NUMBER");
			f.setAccessible(true);
			return f.getInt(null);
	}

    private Long getPingInitialDelay() throws NoSuchFieldException, IllegalAccessException {
        Field f = AgentPingPongPlain.class.getDeclaredField("PING_INITIAL_DELAY");
        f.setAccessible(true);
        return f.getLong(null);
    }

    private Long getPingPeriod() throws NoSuchFieldException, IllegalAccessException {
        Field f = AgentPingPongPlain.class.getDeclaredField("PING_PERIOD");
        f.setAccessible(true);
        return f.getLong(null);
    }

	private Integer extractPingNumber(String consoleOutput) {
		Matcher m = PING_NUMBER_PATTERN.matcher(consoleOutput);
		if (m.find()) {
			return Integer.valueOf(m.group(1));
		}
		return null;
	}

	private int extractSendToCount(String consoleOutput) {
		Matcher m = SEND_TO_PATTERN.matcher(consoleOutput);
		if (!m.find()) {
			return 0;
		}
		String listContent = m.group(1).trim();
		if (listContent.isEmpty()) {
			return 0;
		}

		String[] parts = listContent.split(",");
		int count = 0;
		for (String p : parts) {
			if (!p.trim().isEmpty()) {
				count++;
			}
		}
		return count;
	}

	private int countPingNoOccurrences(String consoleOutput) {
		int count = 0;
		int index = 0;
		String target = "ping-no";
		while ((index = consoleOutput.indexOf(target, index)) != -1) {
			count++;
			index += target.length();
		}
		return count;
	}

    private int countPingOrPongMessages(Pattern pattern, String consoleOutput) {
        Matcher matcher = pattern.matcher(consoleOutput);
        int count = 0;

        while (matcher.find()) {
            count++;
        }
        return count;
    }

	@Test
	public void testExecution_whenUsingConfigurationFromExample() throws Exception {
		Thread th = new Thread(() -> Boot.main(new String[]{}));
		th.start();

		Thread.sleep(1000);
		Integer pingNumber = extractPingNumber(outContent.toString());

		if (pingNumber != null) {
			if (pingNumber >= 0) {
				Thread.sleep(pingNumber * 1000 + 2000);
				String consoleOutput = outContent.toString();
				assertTrue("Number of last ping not found", consoleOutput.contains("[ping-no " + pingNumber + " last"));
				assertEquals(extractSendToCount(consoleOutput) * pingNumber * 4, countPingNoOccurrences(consoleOutput));
			}
			else {
				Thread.sleep(5000);
				String consoleOutput = outContent.toString();
				assertFalse("Number of last ping found", consoleOutput.contains("[ping-no " + pingNumber + " last"));
			}

		}
		else {
			Integer defaultPingNumber = getDefaultPingNumber();

			Thread.sleep(defaultPingNumber * 1000 + 2000);
			String consoleOutput = outContent.toString();
			assertTrue("Number of last ping not found", consoleOutput.contains("[ping-no " + defaultPingNumber + " last"));
			assertEquals(extractSendToCount(consoleOutput) * defaultPingNumber * 4, countPingNoOccurrences(consoleOutput));
		}

	}

    @Test
    public void testExecution_whenOneMessageAndUsingAgentPingPongPlain() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(PING_PONG_CONFIG_ONE_MESSAGE_AGENT_PING_PONG_PLAIN.split(" ")));
        th.start();

        checkCases(true);
    }

    @Test
    public void testExecution_whenFiveMessagesAndUsingAgentPingPongPlain() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(PING_PONG_CONFIG_FIVE_MESSAGES_AGENT_PING_PONG_PLAIN.split(" ")));
        th.start();

        checkCases(true);
    }

    @Test
    public void testExecution_whenTwentyMessagesAndUsingAgentPingPongPlain() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(PING_PONG_CONFIG_TWENTY_MESSAGES_AGENT_PING_PONG_PLAIN.split(" ")));
        th.start();

        checkCases(true);
    }

    @Test
    public void testExecution_whenOneMessageAndUsingAgentPingPong() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(PING_PONG_CONFIG_ONE_MESSAGE_AGENT_PING_PONG.split(" ")));
        th.start();

        checkCases(false);
    }

    @Test
    public void testExecution_whenFiveMessagesAndUsingAgentPingPong() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(PING_PONG_CONFIG_FIVE_MESSAGES_AGENT_PING_PONG.split(" ")));
        th.start();

        checkCases(false);
    }

    @Test
    public void testExecution_whenTwentyMessagesAndUsingAgentPingPong() throws Exception {
        Thread th = new Thread(() -> FlashBoot.main(PING_PONG_CONFIG_TWENTY_MESSAGES_AGENT_PING_PONG.split(" ")));
        th.start();

        checkCases(false);
    }

    private void checkCases(boolean usePlainAgent) throws Exception {
        Long pingInitialDelay = getPingInitialDelay();
        Thread.sleep(pingInitialDelay - 200);

        // before ping initial delay, no message was sent
        String consoleOutput = outContent.toString();
        assertFalse("No message should have been sent", consoleOutput.contains("Sending the message"));

        Thread.sleep(1000);

        // after ping initial delay
        Integer pingNumber = extractPingNumber(outContent.toString());
        Long pingPeriod = getPingPeriod();
        Thread.sleep(pingNumber * pingPeriod + 2000);
        consoleOutput = outContent.toString();

        // check number of ping and pong messages by each agent
        int pingOccurrencesPerMessage;
        int pongOccurrencesPerMessage;
        if (usePlainAgent) {
            pingOccurrencesPerMessage = 6;
            pongOccurrencesPerMessage = 4;
        }
        else {
            pingOccurrencesPerMessage = 4;
            pongOccurrencesPerMessage = 2;
        }
        assertEquals(pingNumber * pingOccurrencesPerMessage, countPingOrPongMessages(AGENT_A_PING_PATTERN, consoleOutput));
        assertEquals(0, countPingOrPongMessages(AGENT_A_PONG_PATTERN, consoleOutput));
        assertEquals(0, countPingOrPongMessages(AGENT_B_PING_PATTERN, consoleOutput));
        assertEquals(pingNumber * pongOccurrencesPerMessage, countPingOrPongMessages(AGENT_B_PONG_PATTERN, consoleOutput));
        assertEquals(0, countPingOrPongMessages(AGENT_C_PING_PATTERN, consoleOutput));
        assertEquals(pingNumber * pongOccurrencesPerMessage, countPingOrPongMessages(AGENT_C_PONG_PATTERN, consoleOutput));
        assertEquals(extractSendToCount(consoleOutput) * pingNumber * 4, countPingNoOccurrences(consoleOutput));

        // check order of ping and pong messages
        for (int i = 1; i < pingNumber - 1; i++) {
            int currentPing = consoleOutput.lastIndexOf("[ping-no " + i + "]");
            int currentPong = consoleOutput.lastIndexOf("[ping-no " + i + " reply]");
            int nextPing = consoleOutput.lastIndexOf("[ping-no " + (i + 1) + "]");
            assertTrue("Wrong sequence of events", currentPing < currentPong);
            assertTrue("Wrong sequence of events", currentPong < nextPing);
        }

        // check the last ping message
        assertTrue("Number of last ping not found", consoleOutput.contains("[ping-no " + pingNumber + " last]"));
    }

}
