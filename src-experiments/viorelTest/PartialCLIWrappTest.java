package viorelTest;

import net.xqhs.flash.testViorel.PartialCLIWrapp;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


import static org.junit.Assert.assertTrue;

public class PartialCLIWrappTest {
    @Test
    public void testProcessArgs(){

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.out.println(new PrintStream(outContent));

        String[] arg = {
                "-package", "testing", "-loader", "agent:composite",
                "-node", "node1", "-agent", "composite:AgentA",
                "-shard", "messaging", "-shard", "PingTest",
                "otherAgent:AgentB", "-shard", "EchoTesting"
        };

        PartialCLIWrapp.processArgs(arg);

        System.setOut(System.out);

        String output = outContent.toString();


        assertTrue(output.contains("Expected output" + output));
    }
}
