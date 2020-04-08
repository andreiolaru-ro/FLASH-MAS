package interfaceGeneratorTest;

import net.xqhs.flash.FlashBoot;

public class DeploymentPageTest {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("No args provided");
            return;
        }

        String test_args = "";
        test_args += " -agent AgentA -shard gui config";
        test_args += " " + args[0];

        FlashBoot.main(test_args.split(" "));
    }
}
