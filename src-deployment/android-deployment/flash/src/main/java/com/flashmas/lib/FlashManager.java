package com.flashmas.lib;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.lifecycle.LiveData;

import net.xqhs.flash.core.agent.Agent;

import java.io.OutputStream;
import java.util.List;

/**
 * Singleton class for usage of Flash Framework on Android platform
 */
public class FlashManager {
    private static FlashManager instance;
    private static Context appContext;

    private FlashManager() throws IllegalStateException {
        if (appContext == null) {
            throw new IllegalStateException("Flash Manager not initialized with application context");
        }
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
        return NodeForegroundService.getLogOutputStream();
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
    }

    public void addAgent() {
        // TODO
    }

    public void removeAgent() {
        // TODO
    }

    public LiveData<List<Agent>> getAgentsLiveData() {
        return NodeForegroundService.getAgentsLiveData();
    }
}
