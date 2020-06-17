package com.flashmas.app;

import android.app.Application;
import android.hardware.Sensor;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.agents.gui.SensorsGuiLinkShard;
import com.flashmas.lib.agents.sensors.SensorsShard;

public class FlashApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        int port = 8882;
        String arg = "-package com.flashmas.lib.agents.gui " +
                "com.flashmas.lib.agents.sensors" +
                " -loader agent:composite -node A -pylon webSocket:slave1 connectTo:ws://192.168.0.172:" + port +
//                "-loader agent:composite -node A -pylon local" +
                " -agent composite:Agent0 -shard messaging -shard SensorsShard " +SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL +
                    " -shard AndroidGuiShard -shard SensorsGuiLinkShard "+ SensorsGuiLinkShard.SHARD_SENSORS_KEY +":" + Sensor.TYPE_ALL;
//
//                " -agent composite:Agent1 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent2 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent3 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent4 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent5 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent6 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent7 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent8 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION +
//
//                " -agent composite:Agent9 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL +
//                " -shard AndroidGuiShard -shard GuiLinkShard SHARD_DESIGNATIONS_KEY:" + SensorsShard.DESIGNATION ;
//                " -agent composite:Agent1 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent2 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent3 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent4 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent5 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent6 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent7 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent8 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent9 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent10 -shard messaging -shard SensorsShard "+SensorsShard.SENSOR_TYPES_ARRAY_KEY+":" + Sensor.TYPE_ALL + " -shard AndroidGuiShard";

//                " -agent composite:Agent0 -shard messaging -shard AndroidGuiShard" +
//                " -agent composite:Agent1 -shard messaging -shard AndroidGuiShard" +
//                " -agent composite:Agent2 -shard messaging -shard AndroidGuiShard" +
//                " -agent composite:Agent3 -shard messaging -shard AndroidGuiShard" +
//                " -agent composite:Agent4 -shard messaging -shard AndroidGuiShard";
        FlashManager.init(this, arg);
    }
}
