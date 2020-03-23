package com.flashmas.lib;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.local.LocalSupport;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;
import net.xqhs.util.logging.wrappers.GlobalLogWrapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.flashmas.lib.Globals.NODE_NAME;

public class NodeForegroundService extends Service {
    private static boolean running = false;
    private static MutableLiveData<Boolean> runningLiveData = new MutableLiveData<>();
    private static OutputStream logsOutputStream = new ByteArrayOutputStream();

    public static void setLogOutputStream(OutputStream s) {
        logsOutputStream = s;
    }

    public static OutputStream getLogOutputStream() {
        return logsOutputStream;
    }

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
        if (running) {
            return;
        }

        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);

        GlobalLogWrapper.setLogStream(logsOutputStream);

        LocalSupport pylon = new LocalSupport();

        for (Agent agent : FlashManager.getInstance().getAgents()) {
            agent.addContext(pylon.asContext());
            agent.start();
        }

        Node localNode = new Node(NODE_NAME);
        localNode.registerEntity("pylon", pylon, pylon.getName());

        FlashManager.getInstance().setMainNode(localNode);
        FlashManager.getInstance().setMainPylon(pylon);

        running = true;
        runningLiveData.postValue(true);
        Toast.makeText(this, "Service started",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (logsOutputStream instanceof ByteArrayOutputStream) {
            ((ByteArrayOutputStream)logsOutputStream).reset();
        }

        FlashManager.getInstance().setMainNode(null);
        running = false;
        runningLiveData.postValue(false);
        Toast.makeText(this, "Service stopped",Toast.LENGTH_LONG).show();
    }

    private Notification buildForegroundNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(notificationManager);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);

        PendingIntent notifPendingIntent = PendingIntent.getActivity(
                this,
                Globals.PendingIntent.FOREGROUND_NOTIF_PENDING_INTENT_ID,
                getPackageManager().getLaunchIntentForPackage(getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        return notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle("Status notification")
                .setContentText("FLASH node is running")
                .setContentIntent(notifPendingIntent)
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

    public static boolean isRunning() {
        return running;
    }

    public static LiveData<Boolean> isRunningLiveData() {
        return runningLiveData;
    }
}
