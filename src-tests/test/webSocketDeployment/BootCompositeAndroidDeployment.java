package test.webSocketDeployment;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing in the following scenario
 * 1 Composite agent on local machine
 * 1 Composite agent on android device with access to local websocket
 */
public class BootCompositeAndroidDeployment
{
    /**
     * Performs test
     *
     * @param args
     *                 - not used.
     */
    public static void main(String[] args)
    {
        String test_args = "";

        test_args += " -package test.compositePingPong -loader agent:composite";

        test_args += " -node node1";
        test_args += " -pylon webSocket:slave1 serverPort:8886";
        test_args += " -agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentB -shard MonitoringTest";

//      ** second node + agent is deployed from the android device **
//        test_args += " -node node2";
//        test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8886";
//        test_args += " -agent composite:AgentB -shard messaging -shard PingBackTest -shard MonitoringTest";

        FlashBoot.main(test_args.split(" "));
    }

}

