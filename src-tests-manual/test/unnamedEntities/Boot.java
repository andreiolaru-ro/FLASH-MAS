package test.unnamedEntities;

import net.xqhs.flash.FlashBoot;

public class Boot {
    /**
    * @param args
    *            - not used.
    */

      public static void main(String[] args) {

        String test_args = "";
        test_args += " -package testing";
        test_args += " -node main";
        test_args += " -agent composite:"; // this will be the kind, no name
        test_args += " -agent : classpath:AgentPingPongPlain"; // bare ':', no kind, no name
        test_args += " -agent - classpath:AgentPingPongPlain";

        FlashBoot.main(test_args.split(" "));
    }
}
