package abms.wolfSheepPredation;

import aggregate_logging.ALogging;
import benchmarking.Benchmark;
import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.abms.Simulation;

public class WolfSheepBoot {
    public static void main(String[] args_) {
        String a = "";
        int scale = 289;
        int steps = 100;

        int width = 6 * (int) Math.sqrt(scale);
        int height = 6 * (int) Math.sqrt(scale);

        int grass = 15 * scale;
        int sheep = 10 * scale;
        int wolf = 5 * scale;
        a += " -load_order simulation;executor;context;WolfSheepGroup";
        a += " -package net.xqhs.flash.abms";
        a += " -package abms.wolfSheepPredation";
        a += " -loader WolfSheepGroup classpath:abms.wolfSheepPredation.WolfSheepGroupLoader";
        a += " -node dummy -simulation sim classpath:Simulation";
        a += " -executor StepWise:StepWise steps:" + steps;
        a += " -context AgentManagement:agentManagement";
        a += " -context Random:random seed:42";
        a += " -context ProximityCommunication:communication";
        a += " -context Space:space width:" + width +" height:" + height;
        a += " -WolfSheepGroup g -patch Grass n:" + grass + " regrowthTime:5 -agent Sheep n:" + sheep
                + " visionRange:2 -agent Wolf n:" + wolf + " visionRange:3";

        FlashBoot.main(a.split(" "));
    }
}