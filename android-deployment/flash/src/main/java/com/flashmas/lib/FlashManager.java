package com.flashmas.lib;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;
import net.xqhs.util.logging.wrappers.GlobalLogWrapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.flashmas.lib.Globals.NODE_NAME;

/**
 * Singleton class for usage of Flash Framework on Android platform
 */
public class FlashManager {
    private static FlashManager instance;
    private static Context appContext;
    private Node deviceNode;
    private Pylon devicePylon;
    private MutableLiveData<List<Agent>> agentsLiveData = new MutableLiveData<>();
    private List<Agent> agentsList = new ArrayList<>(0);
    private static OutputStream logsOutputStream = new ByteArrayOutputStream();

    private String deviceNodeName = NODE_NAME;  // init deviceNodeName with default

    private Observer<Boolean> stateObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean state) {
            updateAgents();
        }
    };


    private FlashManager() throws IllegalStateException {
        if (appContext == null) {
            throw new IllegalStateException("Flash Manager not initialized with application context");
        }
        NodeForegroundService.isRunningLiveData().observeForever(stateObserver);

        //TODO get unique deviceNodeName in FLASHMAS topology
        MultiTreeMap nodeConfig = new MultiTreeMap();
        nodeConfig.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, deviceNodeName);
        deviceNode = new Node(nodeConfig);
        deviceNode.setLogLevel(LoggerSimple.Level.ALL);
        devicePylon = new LocalSupport();
        deviceNode.registerEntity("pylon", devicePylon, devicePylon.getName());
        GlobalLogWrapper.setLogStream(logsOutputStream);
        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);
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

    public static OutputStream getLogOutputStream() {
        return logsOutputStream;
    }

    Node getDeviceNode() {
        return deviceNode;
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

        agent.addContext(devicePylon.<Pylon>asContext());

        if (NodeForegroundService.isRunning()) {
            agent.start();
        }

        deviceNode.registerEntity("agent", agent, agent.getName());
        updateAgents();
    }

    public void removeAgent(Agent agent) {
        if (agent == null)
            return;

        if (agent.isRunning()) {
            agent.stop();
        }

        // TODO deregister agent from node
        // deviceNode.deregisterEntity(...);
        updateAgents();
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
        agentsList = deviceNode.getAgentsList();
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
}
