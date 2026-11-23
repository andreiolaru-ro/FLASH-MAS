package test.templateTest;

import net.xqhs.flash.FlashBoot;

public class Boot
{
    public static void main(String[] args_)
    {
        String args = "";
        args += " -package testing -loader agent:composite";
        args += " -template msg [ -shard messaging ]";
        args += " -node node1";
        args += " -agent composite:AgentA -set msg -shard PingTest otherAgent:AgentB -shard EchoTesting";
        args += " -agent composite:AgentB -set msg -shard PingBackTest -shard EchoTesting";
        FlashBoot.main(args.split(" "));
    }
}