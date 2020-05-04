package com.flashmas.lib.gui;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.flashmas.lib.gui.AndroidGuiShard;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;

public class FlashGuiUtils {
    public static final String GUI_SHARD_DESIGNATION = "gui";
    public static final String TAG = FlashGuiUtils.class.getSimpleName();

    public static void registerGuiEventHandler(CompositeAgent agent, AgentEvent.AgentEventHandler handler) {
        if (agent.asContext() instanceof ShardContainer) {
            AgentShard shard = ((ShardContainer) agent.asContext())
                    .getAgentShard(AgentShardDesignation.autoDesignation(GUI_SHARD_DESIGNATION));
            if (shard instanceof AndroidGuiShard) {
                ((AndroidGuiShard) shard).registerEventHandler(handler);
            }
        }
    }

    public static void unregisterAllAgentGuiHandlers(CompositeAgent agent) {
        if (agent == null) {
            return;
        }

        if (agent.asContext() instanceof ShardContainer) {
            AgentShard shard = ((ShardContainer) agent.asContext())
                    .getAgentShard(AgentShardDesignation.autoDesignation(GUI_SHARD_DESIGNATION));
            if (shard instanceof AndroidGuiShard) {
                ((AndroidGuiShard) shard).unregisterAllEventHandlers();
            }
        }
    }

    public static View getAgentView(CompositeAgent agent, Context context) {
        View agentView = null;

        if (agent == null || context == null) {
            Log.e(TAG, "Agent or context is null");
            return agentView;
        }

        if (agent.asContext() instanceof ShardContainer) {
            AgentShard shard = ((ShardContainer) agent.asContext())
                    .getAgentShard(AgentShardDesignation.autoDesignation(GUI_SHARD_DESIGNATION));
            if (shard instanceof AndroidGuiShard) {
                agentView = ((AndroidGuiShard) shard).getAgentView(context);
            } else {
                Log.e(TAG, "Shard container is not an " + AndroidGuiShard.class.getSimpleName());
            }
        } else {
            Log.e(TAG, "Agent context is not a shard container");
        }

        return agentView;
    }
}
