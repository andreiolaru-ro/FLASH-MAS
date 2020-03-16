package com.flashmas.lib;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;

import java.io.OutputStream;
import java.util.List;

/**
 * Singleton class for usage of Flash Framework on Android platform
 */
public class FlashManager {
    private static FlashManager instance;
    private static Context appContext;
    private static Node mainNode;
    private static MutableLiveData<List<Agent>> agentData = new MutableLiveData<>();

    private Observer<Boolean> stateObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean state) {
            updateAgentsState();
        }
    };


    private FlashManager() throws IllegalStateException {
        if (appContext == null) {
            throw new IllegalStateException("Flash Manager not initialized with application context");
        }
        NodeForegroundService.isRunningLiveData().observeForever(stateObserver);
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

    protected void setMainNode(Node node) {
        mainNode = node;
    }

    public static OutputStream getLogOutputStream() {
        return NodeForegroundService.getLogOutputStream();
    }

    public void startNode() {
        if (mainNode == null) {
            Intent intent = new Intent(appContext, NodeForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent);
            } else {
                appContext.startService(intent);
            }
        }
    }

    public void stopNode() {
        if (mainNode != null) {
            Intent intent = new Intent(appContext, NodeForegroundService.class);
            appContext.stopService(intent);
        }
    }

    public void addAgent(Agent agent) {
        if (mainNode != null) {
            mainNode.registerEntity("Agent", agent, agent.getName());
            updateAgentsState();
        }
    }

    public void removeAgent(Agent agent) {
        // TODO
    }

    public LiveData<Boolean> getRunningLiveData() {
        return NodeForegroundService.isRunningLiveData();
    }

    public LiveData<List<Agent>> getAgentsLiveData() {
        return agentData;
    }

    public void toggleState() {
        if (NodeForegroundService.isRunning()) {
            stopNode();
        } else {
            startNode();
        }
    }

    private void updateAgentsState() {
        if (mainNode == null) {
            agentData.postValue(null);
        } else {
            agentData.postValue(mainNode.getAgents());
        }
    }
}
