package example.composite;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootCompositePingPong {
    /**
     * Performs test
     *
     * @param args_
     *            - not used.
     */
    public static void main(String[] args_)
    {
        String args = "";

        args += " -package testing -loader agent:composite";
        args += " -node node1";
        args += " -agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentB -shard EchoTesting";
        args += " -agent composite:AgentB -shard messaging -shard PingBackTest -shard EchoTesting";

        FlashBoot.main(args.split(" "));
    }

}