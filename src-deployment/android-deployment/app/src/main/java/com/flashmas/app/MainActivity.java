package com.flashmas.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
//        LocalSupport pylon = new LocalSupport();
//
//        TestAgent one = new TestAgent("One");
//        one.addContext(pylon.asContext());
//        TestAgent two = new TestAgent("Two");
//        two.addContext(pylon.asContext());
//
//        one.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
//        two.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
//
//        one.start();
//        two.start();

        Intent intent = new Intent(this, NodeForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
