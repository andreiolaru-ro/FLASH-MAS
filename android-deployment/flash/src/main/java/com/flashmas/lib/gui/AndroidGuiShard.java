package com.flashmas.lib.gui;

import android.util.Log;
import android.util.Pair;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// TODO must extend abstract class GuiShard, from @florinrm
public class AndroidGuiShard extends AgentShardCore {
    private static final String TAG = "Agent GUI Shard";
    private List<AgentEvent.AgentEventHandler> handlerList = new LinkedList<>();
    private String configuration;
    private List<Pair<String, String>> passiveDataInput;

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

    public void sendPassiveInputToShard(List<Pair<String, String>> dataInput) {
        System.out.println("data input" + dataInput);
        passiveDataInput = dataInput;
        System.out.println("passive " + passiveDataInput);
    }

    public void getActiveInput(ArrayList<Pair<String, String>> values) {
        System.out.println("Generating AgentWave for active input...");
        AgentWave wave = new AgentWave(null, "/");
        wave.addSourceElementFirst("/gui/port");
        for (Pair<String, String> value : values) {
            wave.add(value.first, value.second);
        }
        super.getAgent().postAgentEvent(wave);
    }

    public AgentWave getInput(String portName) {

//        AgentWave event = new AgentWave();



        return null;
    }

    public void sendOutput(AgentWave agentWave) {
        String port = agentWave.getCompleteDestination();
        Set<String> roles = agentWave.getKeys();
        roles.remove("EVENT_TYPE");


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
