package abms.wolfSheepPredation;

import net.xqhs.flash.FlashBoot;

public class WolfSheepBoot {
	public static void main(String[] args_) {
		String a = "";

		a += " -load_order context;executor;WolfSheepGroup";
		a += " -package net.xqhs.flash.abms";
		a += " -package abms.wolfSheepPredation";
		a += " -loader WolfSheepGroup classpath:abms.wolfSheepPredation.WolfSheepGroupLoader";
		a += " -node classpath:Simulation";
		a += " -executor StepWise:StepWise steps:10";
		a += " -context Space:space width:6 height:6";
		a += " -WolfSheepGroup g sheepCount:10 wolfCount:5";

		FlashBoot.main(a.split(" "));
	}
}
