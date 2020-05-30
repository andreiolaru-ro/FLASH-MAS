package florina.monitoringAndControlTest;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootComposite {

    /**
     * Perform test.
     * @param args
     *              - not used.
     */
    public static void main(String[] args)
    {
        String test_args = "";

        test_args += " -package florina.monitoringAndControlTest.shards";
        test_args += " -package net.xqhs.flash.core.monitoring.shards";
        test_args += " -loader agent:composite";

        test_args += " -node node1";
        test_args += " -agent composite:AgentA -shard messaging -shard ControlShard -shard MonitoringShard -shard PingBackTestComponent";
        test_args += " -agent composite:AgentB -shard messaging -shard ControlShard -shard MonitoringShard -shard PingTestComponent otherAgent:AgentA";

        FlashBoot.main(test_args.split(" "));
    }
}
