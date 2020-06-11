package com.flashmas.app;

import android.app.Application;

import com.flashmas.lib.FlashManager;

public class FlashApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        int port = 8882;
        String arg = "-package com.flashmas.lib.agents.gui " +
                "com.flashmas.lib.agents.sensors" +
                " -loader agent:composite -node A -pylon webSocket:slave1 connectTo:ws://192.168.0.172:" + port +
//                " -agent composite:Agent0 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent1 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent2 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent3 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL + " -shard AndroidGuiShard" +
//                " -agent composite:Agent4 -shard messaging -shard SensorsShard SENSOR_TYPES_ARRAY_KEY:" + Sensor.TYPE_ALL + " -shard AndroidGuiShard";

                " -agent composite:Agent0 -shard messaging -shard AndroidGuiShard" +
                " -agent composite:Agent1 -shard messaging -shard AndroidGuiShard" +
                " -agent composite:Agent2 -shard messaging -shard AndroidGuiShard" +
                " -agent composite:Agent3 -shard messaging -shard AndroidGuiShard" +
                " -agent composite:Agent4 -shard messaging -shard AndroidGuiShard";
        FlashManager.init(this, arg);
    }
}
