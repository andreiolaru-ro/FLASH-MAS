package net.xqhs.flash.abms;

import net.xqhs.flash.core.agent.Agent;

public interface StepAgent extends Agent {
	void preStep();
	
	void step();
	
}
