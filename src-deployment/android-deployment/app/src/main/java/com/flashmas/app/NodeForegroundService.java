package com.flashmas.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.flashmas.app.agents.TestAgent;

import net.xqhs.flash.local.LocalSupport;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class NodeForegroundService extends Service {
    private LocalSupport pylon;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(Globals.NODE_FOREGROUND_ID, buildForegroundNotification());

        startNode();

        return START_STICKY;
    }

    private void startNode() {
        if (pylon == null) {
            pylon = new LocalSupport();
        } else {
            return;
        }

        TestAgent one = new TestAgent("One");
        one.addContext(pylon.asContext());
        TestAgent two = new TestAgent("Two");
        two.addContext(pylon.asContext());

        one.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
        two.addMessagingShard(new LocalSupport.SimpleLocalMessaging());

        one.start();
        two.start();
        Toast.makeText(this, "Service started",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pylon = null;
        Toast.makeText(this, "Service stopped",Toast.LENGTH_LONG).show();
    }

    private Notification buildForegroundNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(notificationManager);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);

        return notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle("Status notification")
                .setContentText("FLASH node is up and running")
                .build();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager) {
        String channelName = "Flash Node Service";
        NotificationChannel channel = new NotificationChannel(Globals.NODE_NOTIF_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return Globals.NODE_NOTIF_CHANNEL_ID;
    }
}
