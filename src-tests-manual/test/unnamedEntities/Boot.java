package test.unnamedEntities;

import net.xqhs.flash.FlashBoot;

public class Boot {
    /**
    * @param args
    *            - not used.
    */

      public static void main(String[] args) {

        String test_args = "";
		test_args += " -auto_names all:0:agent:5:pylon:node:1 -package testing -loader agent:composite";
		test_args += " -node -pylon local:";
		test_args += " -agent composite: -shard PingTest otherAgent:A -shard messaging";
		test_args += " -agent Test:";
		test_args += " -agent :";
		test_args += " -agent -";
		test_args += " -agent composite:A -shard PingBackTest -shard messaging";
        test_args += " -agent :auto: -agent :auto: -agent composite::auto: -agent Test::auto:";
        test_args += " -agent :auto:bond -agent :auto:bond -agent composite::auto:bond";

        FlashBoot.main(test_args.split(" "));
    }
}
