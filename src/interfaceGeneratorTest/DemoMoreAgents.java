package interfaceGeneratorTest;

import net.xqhs.flash.FlashBoot;

public class DemoMoreAgents {
    public static void main(String[] args) {
        String test_args = "";
        test_args += " -package interfaceGenerator";
        test_args += " -agent AgentA classpath:interfaceGenerator.AgentDemo";
        test_args += " -agent AgentB classpath:interfaceGenerator.AgentDemo";
        FlashBoot.main(test_args.split(" "));
    }
}
