package com.flashmas.lib.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.flashmas.lib.FlashManager;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SensorsAgentShard extends AgentShardCore implements SensorEventListener {
    private static final String TAG = SensorsAgentShard.class.getSimpleName();
    private SensorManager sensorManager;
    private List<Sensor> sensorsList = new LinkedList<>();
    public static final String SENSOR_TYPES_ARRAY_KEY = "SENSOR_TYPES_ARRAY_KEY";

    protected SensorsAgentShard(AgentShardDesignation designation) {
        super(designation);
        sensorManager = (SensorManager) FlashManager.getInstance()
                .getAppContext()
                .getSystemService(Context.SENSOR_SERVICE);
    }

    public SensorsAgentShard() {
        this(AgentShardDesignation.autoDesignation("sensors"));
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        boolean superReturnCode = super.configure(configuration);
        boolean returnCode = false;

        if (!superReturnCode || !configuration.containsSimpleName(SENSOR_TYPES_ARRAY_KEY)) {
            Log.d(TAG, "No sensor provided to shard " + this.getClass().getSimpleName());
            return false;
        }

        List<String> sensorTypes = configuration.getValues(SENSOR_TYPES_ARRAY_KEY);
        for (String sensorType: sensorTypes) {
            int sensorTypeInt = Integer.parseInt(sensorType);

            if (sensorTypeInt == Sensor.TYPE_ALL) {
                sensorsList = sensorManager.getSensorList(Sensor.TYPE_ALL);
                returnCode = true;
                break;
            } else {
                Sensor sensor = sensorManager.getDefaultSensor(sensorTypeInt);
                if (sensor != null) {
                    sensorsList.add(sensor);
                    returnCode = true;
                }
            }
        }

        if (!returnCode) {
            Log.d(TAG, "No sensor found to add from the provided sensor types");
        } else {
            Log.d(TAG, "Sensors added: " + sensorsList);
        }

        return returnCode;
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
        boolean superReturnCode = super.start();
        for (Sensor sensor: sensorsList) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return superReturnCode;
    }

    @Override
    public boolean stop() {
        boolean superReturnCode = super.stop();

        for (Sensor sensor: sensorsList) {
            sensorManager.unregisterListener(this, sensor);
        }
        return superReturnCode;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "Sensor " + event.sensor.getName() + " changed. New values are: " + Arrays.toString(event.values));
        getAgent().postAgentEvent(new AgentWave("Sensor " + event.sensor.getName() + " changed. New values are: " + Arrays.toString(event.values) + " from agent " + getAgent().getEntityName(), "/"));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing for now
    }
}
