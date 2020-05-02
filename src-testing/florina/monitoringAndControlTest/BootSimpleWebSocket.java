package florina.monitoringAndControlTest;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootSimpleWebSocket {

    /**
     * Performs test.
     * @param args
     *              - not used.
     */
    public static void main(String[] args)
    {
        String test_args = "";

		test_args += " -package  florina.monitoringAndControlTest";

		test_args += " -node node1";
        test_args += " -pylon webSocket:slave1 serverPort:8886 connectTo:ws://localhost:8886";
		test_args += " -agent AgentA classpath:AgentTest";
		test_args += " -agent AgentB classpath:AgentTest";

		test_args += " -node node2";
        test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8886";
		test_args += " -agent AgentC classpath:AgentTest sendTo:AgentA";

        FlashBoot.main(test_args.split(" "));
    }

}
