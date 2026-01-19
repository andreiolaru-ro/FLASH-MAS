package abms.wolfSheepPredation;

import net.xqhs.flash.FlashBoot;

public class WolfSheepBoot {
	public static void main(String[] args_) {
		String a = "";

		a += " -load_order map;executor;WolfSheepGroup";
		a += " -package net.xqhs.flash.abms";
		a += " -package abms.wolfSheepPredation";
		a += " -loader WolfSheepGroup classpath:abms.wolfSheepPredation.WolfSheepGroupLoader";
		a += " -node main";
		a += " -map Grid width:6 height:6 classpath:gridworld.GridTopology";
		a += " -executor StepWise steps:10 classpath:net.xqhs.flash.abms.StepWiseExecutor";
		a += " -WolfSheepGroup g sheepCount:10 wolfCount:5";

		FlashBoot.main(a.split(" "));
	}
}
