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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class NodeForegroundService extends Service {
    private static boolean running = false;
    private static MutableLiveData<List<Agent>> agentData = new MutableLiveData<>();

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

        running = true;

        String test_args = "";
        test_args += " -package deploymentTest -loader agent:composite";
        test_args += " -agent composite:AgentA -shard PingTestComponent -shard MonitoringTestShard";
        test_args += " -agent composite:AgentB -shard PingBackTestComponent -shard MonitoringTestShard";

        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);

        NodeLoader nodeLoader = new NodeLoader();

        List<Node> nodes = nodeLoader.loadDeployment(Arrays.asList(test_args.split(" ")));

        List<Agent> agents = new LinkedList<>();
        for(Node node : nodes) {
            node.start();
            agents.addAll(node.getAgents());
        }

        agentData.postValue(agents);

        Toast.makeText(this, "Service started",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        running = false;
        // Mark agents list as null
        agentData.postValue(null);

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

    public static LiveData<List<Agent>> getAgentsLiveData() {
        return agentData;
    }

    public static boolean isRunning() {
        return running;
    }
}
