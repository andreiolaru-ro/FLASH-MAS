package test.deployment.select;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment with select
 */
public class Boot {

    /**
     * Main method. Performs test
     *
     * @param args
     *              -not used.
     */
    public static void main(String[] args) {
		boolean addNoStart = false;
		String test_args = (addNoStart ? "-nostart true " : "")
				+ "-select node1 node3 -node node1 -node node2 -node node3 -node node4";

        FlashBoot.main(test_args.split(" "));
    }
}
