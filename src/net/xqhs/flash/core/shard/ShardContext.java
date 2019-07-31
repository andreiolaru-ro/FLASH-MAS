package net.xqhs.flash.core.shard;

import java.util.List;

import net.xqhs.flash.core.Entity.Context;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.support.Pylon;

public interface ShardContext extends Context<Agent>
{
	void postAgentEvent(AgentEvent event);

	List<Context<Pylon>> getPylons();

	String getAgentName();
}
