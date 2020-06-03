package com.flashmas.lib.agents.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.agents.gui.AgentGuiElement;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SensorsShard extends AgentShardCore implements SensorEventListener, AgentGuiElement {
    private static final String TAG = SensorsShard.class.getSimpleName();
    public static final String DESIGNATION = "sensors";
    public static final String SENSOR_TYPES_ARRAY_KEY = "SENSOR_TYPES_ARRAY_KEY";
    private SensorManager sensorManager;
    private List<Sensor> sensorsList = new LinkedList<>();
    private HashMap<Sensor, TextView> viewMap = new HashMap<>();
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
        if (hasGui && viewMap.containsKey(event.sensor)) {
            uiHandler.post(() ->
                    viewMap.get(event.sensor).setText(Arrays.toString(event.values)));
        } else {
            getAgent().postAgentEvent(new AgentWave("Sensor " + event.sensor.getName() +
                    " changed. New values are: " + Arrays.toString(event.values) +
                    " from agent " + getAgent().getEntityName(), "/"));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing for now
    }

    @Override
    public View getView(Context context) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        for (Sensor s : sensorsList) {
            TextView label = new TextView(context);
            label.setText(s.getName() + ": ");
            ll.addView(label);

            TextView v = new TextView(context);
            viewMap.put(s, v);
            ll.addView(v);
        }
        hasGui = true;
        return ll;
    }
}
