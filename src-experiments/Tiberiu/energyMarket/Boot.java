package Tiberiu.energyMarket;

import net.xqhs.flash.FlashBoot;

/**
 * Entry point for the Local Energy Market simulation.
 * Boots the MAS platform using the specified deployment configuration.
 */
public class Boot {

    public static void main(String[] args_) {
        System.out.println("Starting Local Energy Market Simulation...");
        FlashBoot.main(new String[] { "src-experiments/Tiberiu/energyMarket/deployment.xml" });
    }

}