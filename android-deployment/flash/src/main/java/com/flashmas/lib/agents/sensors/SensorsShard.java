package com.flashmas.lib.agents.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.agents.gui.AgentGuiElement;
import com.flashmas.lib.agents.gui.AndroidGuiShard;
import com.flashmas.lib.agents.gui.generator.Element;
import com.flashmas.lib.agents.gui.generator.ElementType;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import interfaceGenerator.io.IOShard;

public class SensorsShard extends IOShard implements SensorEventListener, AgentGuiElement {
    private static final String TAG = SensorsShard.class.getSimpleName();
    public static final String DESIGNATION = "sensors";
    public static final String SENSOR_TYPES_ARRAY_KEY = "SENSOR_TYPES_ARRAY_KEY";
    private SensorManager sensorManager;
    private List<Sensor> sensorsList = new LinkedList<>();
    private HashMap<Integer, float[]> sensorsValues = new HashMap<>();
    private boolean hasGui = false;
    private static Handler uiHandler = new Handler(Looper.getMainLooper());

    protected SensorsShard(AgentShardDesignation designation) {
        super(designation);
        sensorManager = (SensorManager) FlashManager.getInstance()
                .getAppContext()
                .getSystemService(Context.SENSOR_SERVICE);
    }

    public SensorsShard() {
        this(AgentShardDesignation.autoDesignation(DESIGNATION));
    }

    @Override
    public AgentWave getInput(String sensorType) {
        AgentWave wave = new AgentWave();

        wave.addSourceElementFirst(DESIGNATION);
        wave.add("Value", Arrays.toString(sensorsValues.get(Integer.valueOf(sensorType))));

        return wave;
    }

    @Override
    public void sendOutput(AgentWave agentWave) {
        // Not necessary here
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

        AgentWave wave = new AgentWave();
        wave.addSourceElementFirst(DESIGNATION);
        wave.add(AndroidGuiShard.KEY_PORT, DESIGNATION);
        wave.add(AndroidGuiShard.KEY_ROLE, String.valueOf(event.sensor.getType()));
        wave.add("content", Arrays.toString(event.values));
        getAgent().postAgentEvent(wave);

        sensorsValues.put(event.sensor.getType(), event.values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing for now
    }

    @Override
    public Element getAgentGuiElement() {
        Element container = new Element();
        container.setType(ElementType.BLOCK.type);
        List<Element> children = new LinkedList<>();

        for (Sensor s : sensorsList) {
            Element label = new Element();
            label.setType(ElementType.LABEL.type);
            label.setText(s.getName() + ": ");
            children.add(label);

            Element v = new Element();
            v.setType(ElementType.LABEL.type);
            v.setPort(DESIGNATION);
            v.setRole(String.valueOf(s.getType()));

            children.add(v);
        }
        hasGui = true;
        container.setChildren(children);

        return container;
    }


}
