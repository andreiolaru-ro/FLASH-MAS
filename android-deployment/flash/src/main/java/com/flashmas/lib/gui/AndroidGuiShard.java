package com.flashmas.lib.gui;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import com.flashmas.lib.gui.generator.UiViewFactory;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO must extend abstract class GuiShard, from @florinrm
public class AndroidGuiShard extends AgentShardCore {
    private static final String TAG = "Agent GUI Shard";
    private List<AgentEvent.AgentEventHandler> handlerList = new LinkedList<>();
    private String configuration;
    private View agentView = null;

    protected AndroidGuiShard(AgentShardDesignation designation) {
        super(designation);
    }

    public AndroidGuiShard() {
        this(AgentShardDesignation.autoDesignation("gui"));
    }

    public AndroidGuiShard(MultiTreeMap configuration) {
        this();
        this.configuration = configuration.getSingleTree("config").getTreeKeys().get(0);
    }

    public View getAgentView(Context context) {
        if (agentView != null) {
            return agentView;
        }

        if (configuration != null && !configuration.isEmpty()) {
            // TODO get yaml from configuration
        } else {
            try {
                InputStream inputStream = context.getAssets().open("agent_view2.yaml");
                agentView = UiViewFactory.parseAndCreateView(inputStream, context, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return agentView;
    }

    public void onAgentWave(AgentWave agentWave) {
        Log.d(TAG, "Received agentWave: " + agentWave);

//        String port = agentWave.getCompleteDestination();
//        Set<String> roles = agentWave.getKeys();
//        roles.remove("EVENT_TYPE");

//        Log.d(TAG, "Roles are: " + roles);
//        for (String role : roles) {
////            List<String> elementsFromPort = IdResourceManager.findElementsByRole(PageBuilder.getInstance().getPage(), role);
//            List<String> values = agentWave.getValues(role);
//
////            if (elementsFromPort.size() == 0) {
////                continue;
////            }
//        }

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

    public void onActiveInput(String id, String role, String port) {
        Log.d(TAG, "received active input from id=" + id + " with action=" + role + " and port=" + port);

        // Depending on the role of the input, make an event
        switch (role) {
            case "send":
                AgentWave wave = buildAgentWave(port);
                super.getAgent().postAgentEvent(wave);
                break;
            case "move":
            default:
        }
    }

    private AgentWave buildAgentWave(String port) {
        AgentWave wave = new AgentWave(null, "/");
        wave.addSourceElementFirst("/gui/port");

        Map<String, String> formMap = IdResourceManager.getPortValues(agentView, port);
        for (String formName : formMap.keySet()) {
            wave.add(formName, formMap.get(formName));
        }

        Log.d(TAG, "Built the wave: " + wave);

        return wave;
    }
}
