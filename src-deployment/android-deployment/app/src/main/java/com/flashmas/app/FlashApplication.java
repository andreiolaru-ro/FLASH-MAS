package com.flashmas.app;

import android.app.Application;

import com.flashmas.lib.FlashManager;

public class FlashApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FlashManager.init(this);
    }
}
