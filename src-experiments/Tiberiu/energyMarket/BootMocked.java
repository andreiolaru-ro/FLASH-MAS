package Tiberiu.energyMarket;

import net.xqhs.flash.FlashBoot;

/**
 * Entry point for the HYBRID Local Energy Market simulation.
 * Boots the MAS platform using the mocked deployment configuration,
 * blending live agents with historical playback agents.
 */
public class BootMocked {

    public static void main(String[] args_) {
        System.out.println("=========================================================");
        System.out.println("Starting HYBRID (Mocked) Energy Market Simulation...");
        System.out.println("Loading Live and Playback Agents...");
        System.out.println("=========================================================");

        // Ensure the path matches the new mocked XML file
        FlashBoot.main(new String[] { "src-experiments/Tiberiu/energyMarket/deploymentMocked.xml" });
    }

}