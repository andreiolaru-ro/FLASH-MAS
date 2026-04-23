package abms.lbForaging;

import net.xqhs.flash.FlashBoot;

/**
 * Entry point for the Level-Based Foraging simulation.
 *
 * Setup: 8x8 grid, 3 forager agents (levels 1-2), 5 food items (levels 1-3).
 * Episode: 100 steps. Food collected when sum of adjacent loading agents' levels >= food level.
 * Learning: Independent Q-Learning (tabular) with epsilon-greedy exploration.
 */
public class LBForagingBoot {
    public static void main(String[] args_) {
        String a = "";

        a += " -load_order simulation;executor;context;LBForagingGroup";
        a += " -package net.xqhs.flash.abms";
        a += " -package abms.lbForaging";
        a += " -loader LBForagingGroup classpath:abms.lbForaging.LBForagingGroupLoader";
        a += " -node dummy -simulation sim classpath:Simulation";
        a += " -executor StepWise:StepWise steps:100";
        a += " -context AgentManagement:agentManagement";
        a += " -context Random:random seed:42";
        a += " -context ProximityCommunication:communication";
        a += " -context Space:space width:8 height:8";
        a += " -context Foraging:foraging";
        a += " -LBForagingGroup g";
        a += " -patch Food n:5 level:2";
        a += " -agent Forager n:3 level:1 visionRange:2";

        FlashBoot.main(a.split(" "));
    }
}
