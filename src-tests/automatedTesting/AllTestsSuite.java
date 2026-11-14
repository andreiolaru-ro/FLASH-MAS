package automatedTesting;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite that runs all tests in a specific order.
 * This ensures tests are executed in the desired sequence, not alphabetically.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    CompositeAgentTest.class,
    HelloWorldAgentTest.class,
    ConfigurableHelloWorldAgentTest.class,
    AgentPingPongTest.class,
    AgentPingPongWebSocketTest.class,
    AgentMobilityWebSocketTest.class
})
public class AllTestsSuite {
    // This class remains empty, it is used only as a holder for the above annotations
}

