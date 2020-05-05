package com.flashmas.lib.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.flashmas.lib.FlashManager;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

public class SensorsAgentShard extends AgentShardCore implements SensorEventListener {
    private static final String TAG = SensorsAgentShard.class.getSimpleName();
    private SensorManager sensorManager;
    private Sensor pressureSensor;

    protected SensorsAgentShard(AgentShardDesignation designation) {
        super(designation);
        sensorManager = (SensorManager) FlashManager.getInstance()
                .getAppContext()
                .getSystemService(Context.SENSOR_SERVICE);
        // TODO check configuration for which sensors to have
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    public SensorsAgentShard() {
        this(AgentShardDesignation.autoDesignation("sensors"));
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
        Log.d(TAG, "start");
        boolean returnCode = super.start();
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        return returnCode;
    }

    @Override
    public boolean stop() {
        Log.d(TAG, "stop");
        boolean returnCode = super.stop();
        sensorManager.unregisterListener(this, pressureSensor);
        return returnCode;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float millibarsOfPressure = event.values[0];
        Log.d(TAG, "Pressure is: " + millibarsOfPressure);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing for now
    }
}
