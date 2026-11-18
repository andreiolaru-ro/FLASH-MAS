package test.unnamedEntities;

import net.xqhs.flash.FlashBoot;

public class Boot {
    /**
    * @param args
    *            - not used.
    */

      public static void main(String[] args) {

        String test_args = "";
		test_args += " -package testing -loader agent:composite";
		test_args += " -node -pylon local:";
		test_args += " -agent composite: -shard PingTest otherAgent:A -shard messaging";
		test_args += " -agent Test:";
		test_args += " -agent :";
		test_args += " -agent -";
		test_args += " -agent composite:A -shard PingBackTest -shard messaging";

        FlashBoot.main(test_args.split(" "));
    }
}
