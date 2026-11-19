package abms.samples;

import net.xqhs.flash.FlashBoot;

public class BootSamples {
	
	enum Method {
		STEP,
	}
	
	public static Method	METHOD		= Method.STEP;
	public static boolean	SIMULATE	= false;
	
	public static void main(String[] args) {
		String agent = "";
		switch(METHOD) {
		case STEP:
			agent = "stepper";
			break;
		}
		
		String a = "";
		if(SIMULATE) {
			int steps = 50;
			a += " -load_order executor;context;agent";
			a += " -package net.xqhs.flash.abms abms.samples";
			a += " -node sim classpath:Simulation central:web";
			a += "    -executor StepWise:StepWise steps:" + steps;
			a += "     -context AgentManagement:agentManagement";
			a += "     -context GeneralCommunication:communication";
			a += "      -agent " + agent + ":A sendTo:B -shard remoteOperation";
			a += "      -agent " + agent + ":B -shard remoteOperation";
		}
		else {
			a += " -package abms.samples";
			a += " -node dummy central:web";
			a += "  -agent " + agent + ":A";
			a += "  -agent " + agent + ":B sendTo:A";
		}
		FlashBoot.main(a.split(" "));
		
	}
}
