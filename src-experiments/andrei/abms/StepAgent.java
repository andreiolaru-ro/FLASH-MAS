package andrei.abms;

import net.xqhs.flash.core.agent.BaseAgent;

public abstract class StepAgent extends BaseAgent {
	abstract void preStep();
	
	abstract void step();
	
}
