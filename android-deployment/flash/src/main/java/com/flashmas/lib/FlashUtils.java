package com.flashmas.lib;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;

public class FlashUtils {
    public static final String GUI_SHARD_DESIGNATION = "gui";

    public static void registerGuiEventHandler(CompositeAgent agent, AgentEvent.AgentEventHandler handler) {
        if (agent.asContext() instanceof ShardContainer) {
            AgentShard shard = ((ShardContainer) agent.asContext())
                    .getAgentShard(AgentShardDesignation.autoDesignation(GUI_SHARD_DESIGNATION));
            if (shard instanceof AgentGuiShard) {
                ((AgentGuiShard) shard).registerEventHandler(handler);
            }
        }
    }

    public static void unregisterAllAgentGuiHandlers(CompositeAgent agent) {
        if (agent.asContext() instanceof ShardContainer) {
            AgentShard shard = ((ShardContainer) agent.asContext())
                    .getAgentShard(AgentShardDesignation.autoDesignation(GUI_SHARD_DESIGNATION));
            if (shard instanceof AgentGuiShard) {
                ((AgentGuiShard) shard).unregisterAllEventHandlers();
            }
        }
    }
}
