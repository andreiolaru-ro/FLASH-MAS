package automatedTesting;

import example.simplePingPong.Boot;
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

	private static final Pattern PING_NUMBER_PATTERN =
			Pattern.compile("\\[ping-number\\]>\\s*\\[(-?\\d+)\\]");

	private static final Pattern SEND_TO_PATTERN =
			Pattern.compile("\\[sendTo\\]>\\s*\\[([^\\]]*)\\]");

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

	@Test
	public void testExecution() throws Exception {
		Thread bootThread = new Thread(() -> Boot.main(new String[]{}));
		bootThread.start();

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


}
