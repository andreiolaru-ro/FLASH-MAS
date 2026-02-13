package net.xqhs.flash.abms;

import net.xqhs.flash.core.agent.Agent;

public interface SimulationExecutor {
	public boolean register(Agent agent);
	
	public boolean register(AgentGroup group);
}
