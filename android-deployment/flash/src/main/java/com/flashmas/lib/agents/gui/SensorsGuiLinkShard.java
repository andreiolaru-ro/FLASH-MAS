package com.flashmas.lib.agents.gui;

import com.flashmas.lib.agents.sensors.SensorsShard;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import interfaceGenerator.io.IOShard;

public class SensorsGuiLinkShard extends AgentShardCore {
    private MultiTreeMap config = null;
    public static final String DESIGNATION = "gui_link_shards";
    public static final String SHARD_SENSORS_KEY = "shards";
    private Thread updateGuiThread = null;
    private AtomicBoolean running = new AtomicBoolean(false);

    protected SensorsGuiLinkShard(AgentShardDesignation designation) {
        super(designation);
    }

    public SensorsGuiLinkShard() {
        this(AgentShardDesignation.autoDesignation(DESIGNATION));
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        super.configure(configuration);
        config = configuration;
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        switch (event.getType()) {
            case AGENT_START:
                start();
                break;
            case AGENT_STOP:
                stop();
                break;
            default:
                // Nothing
        }
    }

    @Override
    public boolean start() {
        super.start();
        AgentShard sensorsShard = getAgent().getAgentShard(
                AgentShardDesignation.autoDesignation(SensorsShard.DESIGNATION));
        AgentShard guiShard = getAgent().getAgentShard(
                AgentShardDesignation.autoDesignation(AndroidGuiShard.DESIGNATION));
        running.set(true);

        if (guiShard instanceof IOShard && sensorsShard instanceof IOShard) {
            updateGuiThread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    AgentWave wave;
                    while (running.get()) {
                        if (config != null && config.containsKey(SHARD_SENSORS_KEY)) {
                            List<String> types = config.getValues(SHARD_SENSORS_KEY);
                            for (String type: types) {
                                wave = ((IOShard) sensorsShard).getInput(type);
                                wave.add(AndroidGuiShard.KEY_PORT, "sensors");
                                wave.add(AndroidGuiShard.KEY_ROLE, "output");
                                ((IOShard) guiShard).sendOutput(wave);
                            }
                        }
                        try {
                            sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            updateGuiThread.start();
            return true;
        }

        return false;
    }

    @Override
    public boolean stop() {
        super.stop();

        running.set(false);

        if (updateGuiThread != null) {
            try {
                updateGuiThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateGuiThread = null;
        }
        return true;
    }
}
