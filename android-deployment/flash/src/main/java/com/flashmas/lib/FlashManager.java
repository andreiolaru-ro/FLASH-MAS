package com.flashmas.lib;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.flashmas.lib.agents.gui.AndroidGuiShard;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class for usage of Flash Framework on Android platform
 */
public class FlashManager {
    public static final String TAG = FlashManager.class.getSimpleName();
    private static FlashManager instance;
    private static Context appContext;
    private MutableLiveData<List<Agent>> agentsLiveData = new MutableLiveData<>();
    private List<Agent> agentsList = new ArrayList<>(0);


    private FlashManager() throws IllegalStateException {
        if (appContext == null) {
            throw new IllegalStateException("FlashManager not initialized with application context");
        }
    }

    public static void init(Context context) {
        if (context == null || appContext != null) {
            return;
        }

        appContext = context.getApplicationContext();
    }

    public static FlashManager getInstance() throws IllegalStateException {
        if (instance == null) {
            synchronized (FlashManager.class) {
                if (instance == null) {
                    instance = new FlashManager();
                }
            }
        }

        return instance;
    }

    public OutputStream getLogOutputStream() {
        return NodeForegroundService.logsOutputStream;
    }

    public void startNode() {
        Intent intent = new Intent(appContext, NodeForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    public void stopNode() {
        Intent intent = new Intent(appContext, NodeForegroundService.class);
        appContext.stopService(intent);
        stopAgents();
    }

    private void stopAgents() {
        for (Agent agent: agentsList) {
            agent.stop();
        }
    }

    public void addAgent(Agent agent) {
        if (agent == null)
            return;

        agentsList.add(agent);
        updateAgents();
    }

    public void removeAgent(Agent agent) {
        // TODO
//        if (agent == null)
//            return;
//
//        if (agent.isRunning()) {
//            agent.stop();
//        }
//
//        // TODO deregister agent from node
//        // deviceNode.deregisterEntity(...);
//        updateAgents();
    }

    public LiveData<Boolean> getRunningLiveData() {
        return NodeForegroundService.isRunningLiveData();
    }

    public LiveData<List<Agent>> getAgentsLiveData() {
        return agentsLiveData;
    }

    public void toggleState() {
        if (NodeForegroundService.isRunning()) {
            stopNode();
        } else {
            startNode();
        }
    }

    private void updateAgents() {
        agentsLiveData.postValue(agentsList);
    }

    public List<Agent> getAgentsList() {
        return agentsList;
    }

    public Agent getAgent(String name) {
        for (Agent agent: agentsList) {
            if (agent.getName().equals(name)) {
                return agent;
            }
        }

        return null;
    }

    public Context getAppContext() {
        return appContext;
    }

    public View getAgentView(CompositeAgent agent) {
        View agentView = null;

        if (agent == null) {
            Log.e(TAG, "Agent is null");
            return agentView;
        }

        if (agent.asContext() instanceof ShardContainer) {
            AgentShard shard = ((ShardContainer) agent.asContext())
                    .getAgentShard(AgentShardDesignation.autoDesignation(AndroidGuiShard.DESIGNATION));
            if (shard instanceof AndroidGuiShard) {
                agentView = ((AndroidGuiShard) shard).getAgentView(appContext);
            } else {
                Log.e(TAG, "Shard container is not an " + AndroidGuiShard.class.getSimpleName());
            }
        } else {
            Log.e(TAG, "Agent context is not a shard container");
        }

        return agentView;
    }

    public View getAgentView(String agentName) {
        View agentView = null;

        if (agentName == null) {
            Log.e(TAG, "Agent is null");
            return null;
        }

        Agent agent = FlashManager.getInstance().getAgent(agentName);
        if (agent instanceof CompositeAgent) {
            agentView = getAgentView((CompositeAgent) agent);
        }
        return agentView;
    }
}
