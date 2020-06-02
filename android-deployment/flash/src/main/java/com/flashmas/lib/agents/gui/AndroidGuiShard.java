package com.flashmas.lib.agents.gui;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.flashmas.lib.agents.gui.generator.Element;
import com.flashmas.lib.agents.gui.generator.UiViewFactory;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// TODO must extend abstract class GuiShard, from @florinrm
public class AndroidGuiShard extends AgentShardCore {
    private static final String TAG = AndroidGuiShard.class.getSimpleName();
    public static final String DESIGNATION = "gui";
    private List<AgentEvent.AgentEventHandler> handlerList = new LinkedList<>();
    private MultiTreeMap configuration;
    private View agentView = null;

    protected AndroidGuiShard(AgentShardDesignation designation) {
        super(designation);
    }

    public AndroidGuiShard() {
        this(AgentShardDesignation.autoDesignation(DESIGNATION));
    }

    public AndroidGuiShard(MultiTreeMap configuration) {
        this();
        this.configuration = configuration;
    }

    public View getAgentView(Context context) {
        if (agentView != null) {
            return agentView;
        }

        if (configuration != null) {
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

    public void onActiveInput(int id, String role, String port) {
        Log.d(TAG, "received active input from id=" + id + " with action=" + role + " and port=" + port);

        // Depending on the role of the input, make an event
        switch (role) {
            case "send":
            case "move":
            default:
                AgentWave wave = buildAgentWave(IdResourceManager.getElement(id));
                super.getAgent().postAgentEvent(wave);
                break;
        }
    }

    private AgentWave buildAgentWave(Element element) {
        if (element == null) {
            return null;
        }
        AgentWave wave = new AgentWave(null, "/");
        wave.addSourceElementFirst("/gui/" + element.getPort());

        Map<Integer, String> formMap = IdResourceManager.getPortValues(agentView, element.getPort());
        for (Integer elementId : formMap.keySet()) {
            wave.add(IdResourceManager.getElement(elementId).getRole(), formMap.get(elementId));
        }

        Log.d(TAG, "Built the wave: " + wave);

        return wave;
    }

    public ShardContainer getShardContainer() {
        return getAgent();
    }
}
