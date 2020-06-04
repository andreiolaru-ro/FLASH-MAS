package com.flashmas.lib.agents.gui;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.flashmas.lib.agents.gui.generator.AgentViewFactory;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import interfaceGenerator.io.IOShard;

import static com.flashmas.lib.agents.gui.IdResourceManager.buildAgentWave;

public class AndroidGuiShard extends IOShard {
    private static final String TAG = AndroidGuiShard.class.getSimpleName();
    public static final String DESIGNATION = "gui";
    public static final String KEY_PORT = "port";   // TODO move to IOShard
    public static final String KEY_ROLE = "role";   // TODO move to IOShard
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

    @Override
    public AgentWave getInput(String port) {
        return buildAgentWave(agentView, port);
    }

    @Override
    public void sendOutput(AgentWave agentWave) {
        String port = agentWave.get(KEY_PORT);
        String role = agentWave.get(KEY_ROLE);
        Integer id = IdResourceManager.getId(port, role);
        if (id == null) {
            return;
        }

        View v = agentView.findViewById(id);
        String content = agentWave.getContent();

        if (v instanceof EditText) {
            ((EditText)v).setText(content);
        } else if (v instanceof Button) {
            ((Button)v).setText(content);
        } else if (v instanceof TextView) {
            ((TextView)v).setText(content);
        }
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
                agentView = AgentViewFactory.parseAndCreateView(inputStream, context, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return agentView;
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

    public void onActiveInput(int id, String role, String port) {
        Log.d(TAG, "received active input from id=" + id + " with action=" + role + " and port=" + port);

        // Depending on the role of the input, make an event
        switch (role) {
            case "send":
            case "move":
            default:
                AgentWave wave = buildAgentWave(agentView, IdResourceManager.getElement(id).getPort());
                super.getAgent().postAgentEvent(wave);
                break;
        }
    }

    public ShardContainer getShardContainer() {
        return getAgent();
    }
}
