package abms.samples;

import net.xqhs.flash.FlashBoot;

public class BootSamples {
	
	enum Method {
		STEP,
	}
	
	public static Method	METHOD		= Method.STEP;
	public static boolean	SIMULATE	= true;
	
	public static void main(String[] args) {
		String agent = "";
		switch(METHOD) {
		case STEP:
			agent = "stepper";
			break;
		}
		
		String a = "";
		if(SIMULATE) {
			int steps = 100;
			a += " -load_order executor;pylon;agent";
			a += " -package net.xqhs.flash.abms abms.samples";
			a += " -node sim classpath:Simulation";
			a += "    -executor StepWise:StepWise steps:" + steps;
			a += "      -pylon local:pyl";
			a += "      -agent " + agent + ":A sendTo:B";
			a += "      -agent " + agent + ":B";
		}
		else {
			a += " -package abms.samples";
			a += " -node dummy";
			a += "  -agent " + agent + ":A";
			a += "  -agent " + agent + ":B sendTo:A";
		}
		FlashBoot.main(a.split(" "));
		
	}
}
