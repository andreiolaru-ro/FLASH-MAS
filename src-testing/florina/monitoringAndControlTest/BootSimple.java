package florina.monitoringAndControlTest;

import net.xqhs.flash.FlashBoot;

public class BootSimple {

    /**
     * Performs test.
     *
     * @param args
     *                 - not used.
     */
    public static void main(String[] args)
    {
        String test_args = "";

        test_args += " -package  florina.monitoringAndControlTest";

        test_args += " -node node1";
        //test_args += " -node node1 -support local:main use-thread";
        test_args += " -agent AgentA classpath:AgentTest";
        test_args += " -agent AgentB classpath:AgentTest sendTo:AgentA";

        FlashBoot.main(test_args.split(" "));
    }
}
