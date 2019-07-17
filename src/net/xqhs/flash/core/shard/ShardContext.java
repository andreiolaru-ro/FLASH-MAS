package net.xqhs.flash.core.shard;

import net.xqhs.flash.core.Entity.Context;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.composite.AgentEvent;

public interface ShardContext extends Context<Agent>
{
	void postAgentEvent(AgentEvent event);
}
