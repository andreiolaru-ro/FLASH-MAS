package com.flashmas.lib;

import android.util.Log;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

import java.util.LinkedList;
import java.util.List;

public class AgentGuiShard extends AgentShardCore {
    private static final String TAG = "Agent GUI Shard";
    private List<AgentEvent.AgentEventHandler> handlerList = new LinkedList<>();

    protected AgentGuiShard(AgentShardDesignation designation) {
        super(designation);
    }

    public AgentGuiShard() {
        this(AgentShardDesignation.autoDesignation("gui"));
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);

        signalHandlers(event);
        Log.d(TAG, "signal event: " + event.toString());
    }

    private void signalHandlers(AgentEvent event) {
        for (AgentEvent.AgentEventHandler handler: handlerList) {
            handler.handleEvent(event);
        }
    }

    public void registerEventHandler(AgentEvent.AgentEventHandler handler) {
        if (handler == null)
            return;

        handlerList.add(handler);
    }

    public void unregisterEventHandler(AgentEvent.AgentEventHandler handler) {
        if (handler == null)
            return;

        handlerList.remove(handler);
    }

    public void unregisterAllEventHandlers() {
        for (AgentEvent.AgentEventHandler handler: handlerList) {
            unregisterEventHandler(handler);
        }
    }
}
