package deployment.select;

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
        String test_args = "-select node1 node3 -node node1 -node node2 -node node3 -node node4";

        FlashBoot.main(test_args.split(" "));
    }
}
