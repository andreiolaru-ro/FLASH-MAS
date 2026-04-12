package abms.wolfSheepPredation;

import net.xqhs.flash.FlashBoot;

public class WolfSheepBoot {
    public static void main(String[] args_) {
        String a = "";

        a += " -load_order simulation;executor;context;WolfSheepGroup";
        a += " -package net.xqhs.flash.abms";
        a += " -package abms.wolfSheepPredation";
        a += " -loader WolfSheepGroup classpath:abms.wolfSheepPredation.WolfSheepGroupLoader";
        a += " -node dummy -simulation sim classpath:Simulation";
        a += " -executor StepWise:StepWise steps:100";
        a += " -context AgentManagement:agentManagement";
        a += " -context Random:random seed:42";
        a += " -context ProximityCommunication:communication";
        a += " -context Space:space width:6 height:6";
        a += " -WolfSheepGroup g -agent Sheep n:10 visionRange:2 -agent Wolf n:5 visionRange:3 -agent Grass n:15";

        FlashBoot.main(a.split(" "));
    }
}
