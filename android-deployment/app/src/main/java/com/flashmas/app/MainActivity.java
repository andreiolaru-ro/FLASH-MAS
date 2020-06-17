package com.flashmas.app;

import android.hardware.Sensor;
import android.os.Bundle;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.ui.FlashActivity;

import net.xqhs.flash.core.agent.Agent;

import java.util.Arrays;

public class MainActivity extends FlashActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Agent getCompositeAgent() {
        return FlashManager.getCompositeAgentBuilder()
                .addGuiShard()
                .addSensorShard(Arrays.asList(Sensor.TYPE_ALL))
                .addGuiLinkShard(Arrays.asList(String.valueOf(Sensor.TYPE_GYROSCOPE)))
                .build();
    }
}
