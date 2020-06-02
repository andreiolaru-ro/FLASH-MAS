package com.flashmas.lib;

import android.app.Application;

public class FlashApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FlashManager.init(this);
    }
}
