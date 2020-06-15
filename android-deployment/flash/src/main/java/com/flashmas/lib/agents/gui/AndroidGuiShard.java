package com.flashmas.lib.agents.gui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.agents.gui.generator.AgentViewFactory;
import com.flashmas.lib.agents.gui.generator.Configuration;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import interfaceGenerator.Pair;
import interfaceGenerator.io.IOShard;

public class AndroidGuiShard extends IOShard {
    private static final String TAG = AndroidGuiShard.class.getSimpleName();
    public static final String DESIGNATION = "gui";
    public static final String KEY_PORT = "port";   // TODO move to IOShard
    public static final String KEY_ROLE = "role";   // TODO move to IOShard
    private static final String KEY_FILEPATH = "filepath";
    private List<AgentEvent.AgentEventHandler> handlerList = new LinkedList<>();
    private MultiTreeMap configuration = null;
    private View agentView = null;
    private IdResourceManager idResourceManager = new IdResourceManager();
    private Configuration uiConfig = null;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

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
    public boolean configure(MultiTreeMap configuration) {
        super.configure(configuration);
        String filename = null; // TODO switch to filepath

        if (configuration != null && configuration.containsKey(KEY_FILEPATH)) {
            // TODO get yaml from configuration
        } else {
            filename = "agent_view2.yaml";  // Use basic config
        }

        InputStream inputStream = null;
        try {
            inputStream = FlashManager.getInstance().getAppContext()
                    .getAssets().open(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uiConfig = AgentViewFactory.parseYaml(inputStream);
        idResourceManager.mapElement(uiConfig.getNode());
        return true;
    }

    @Override
    public AgentWave getInput(String port) {
        if (idResourceManager == null) {
            return null;
        }
        return idResourceManager.buildAgentWave(agentView, port);
    }

    @Override
    public void sendOutput(AgentWave agentWave) {
        if (!isRunning()) {
            return;
        }
        String port = agentWave.get(KEY_PORT);
        if (port == null) {
            String[] destinationElements = agentWave.getCompleteDestination().split("/");
            port = destinationElements[destinationElements.length - 1];
        }
        String role = agentWave.get(KEY_ROLE);
        Integer id = idResourceManager.getId(port, role);
        if (id == null) {
            id = idResourceManager.getId(port, "logging");
        }

        if (id == null) {
            return;
        }

        String content = agentWave.getContent();
        idResourceManager.getElement(id).setText(content);

        if (agentView == null) {
            return;
        }
        View v = agentView.findViewById(id);

        uiHandler.post(() -> {
            if (v instanceof EditText) {
                ((EditText)v).setText(content);
            } else if (v instanceof Button) {
                ((Button)v).setText(content);
            } else if (v instanceof TextView) {
                ((TextView)v).setText(content);
            }
        });
    }

    @Override
    public void getActiveInput(ArrayList<Pair<String, String>> values) throws Exception {
        super.getActiveInput(values);
    }

    public View getAgentView(Context context) {
        if (agentView == null) {
            agentView = AgentViewFactory.createView(uiConfig, context, this);
        }

        return agentView;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);

        if (event.getType().compareTo(AgentEvent.AgentEventType.AGENT_START) == 0) {
            start();
        } else if (event.getType().compareTo(AgentEvent.AgentEventType.AGENT_STOP) == 0) {
            stop();
        }
        signalHandlers(event);
        if (event instanceof AgentWave) {
            sendOutput((AgentWave)event);
        }

        Log.d(TAG, "signal event: " + event.toString());
    }

    private void signalHandlers(AgentEvent event) {
        if (!isRunning()) {
            return;
        }
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
        String shardPath = DESIGNATION + "/" + port;
        String source = getAgent().getEntityName() + "/" + shardPath;
        // Depending on the role of the input, make an event
        switch (role) {
            case "send":
            case "move":
            default:
                AgentWave wave = idResourceManager.buildAgentWave(
                        agentView,
                        idResourceManager.getElement(id).getPort()
                );
                AgentShard s = super.getAgent().getAgentShard(AgentShardDesignation.autoDesignation("messaging"));
                if (s instanceof MessagingShard) {
                    ((MessagingShard) s).sendMessage(source, wave.get("target") + "/" + shardPath, wave.get(wave.CONTENT));
                }
                break;
        }
    }

    public ShardContainer getShardContainer() {
        return getAgent();
    }

    public IdResourceManager getIdResourceManager() {
        return idResourceManager;
    }

    public void removeAgentView() {
        agentView = null;
    }
}
