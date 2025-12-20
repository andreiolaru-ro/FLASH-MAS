package test.guiGeneration.distributed;

import net.xqhs.flash.FlashBoot;

public class BootClientNode {

    public static String SERVER_IP = "192.168.1.12";
    public static final int SERVER_PORT = 8886;

    public static void main(String[] args) {
        String test_args = "";

        test_args += " -loader agent:composite";
        test_args += " -package test.guiGeneration testing";

        test_args += " -node nodeB";
        test_args += " -pylon webSocket:clientPylon connectTo:ws://" + SERVER_IP + ":" + SERVER_PORT;

        test_args += " -agent composite:AgentB";
        test_args += " -shard messaging";

        test_args += " -shard swingGui from:basic-chat.yml";
        test_args += " -shard remoteOperation wait:5000";
        test_args += " -shard BasicChat otherAgent:AgentA";

        FlashBoot.main(test_args.split(" "));
    }
}