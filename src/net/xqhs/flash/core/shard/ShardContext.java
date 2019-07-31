package net.xqhs.flash.core.shard;

import java.util.List;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.support.Pylon;

public interface ShardContext extends EntityProxy<Agent>
{
	void postAgentEvent(AgentEvent event);

	List<EntityProxy<Pylon>> getPylons();

	String getAgentName();
}
