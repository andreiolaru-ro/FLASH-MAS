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
import androidx.lifecycle.Observer;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalPylon;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;
import net.xqhs.util.logging.wrappers.GlobalLogWrapper;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.flashmas.lib.Globals.NODE_NAME;

public class NodeForegroundService extends Service {
    public static final String KEY_CONFIG = "key_config";
    private static boolean running = false;
    private static MutableLiveData<Boolean> runningLiveData = new MutableLiveData<>();
    private Node node;
    private List<Pylon> pylonsList = new LinkedList<>();
    private String deviceNodeName = NODE_NAME;  // init deviceNodeName with default
    static ByteArrayOutputStream logsOutputStream = new ByteArrayOutputStream();
    private boolean hasConfig = false;

    Observer<List<Agent>> agentsObserver = new Observer<List<Agent>>() {
        @Override
        public void onChanged(List<Agent> agents) {
            for (Agent agent: agents) {
                // Don't add agent if already registered
                if (node.getEntitiesList().contains(agent)) {
                    continue;
                }

                // Add agent in context of pylons
                for (Pylon pylon: pylonsList) {
                    agent.addContext(pylon.asContext());
                }

                node.registerEntity("agent", agent, agent.getName());

                if (running) {
                    agent.start();
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Globals.NODE_FOREGROUND_ID, buildForegroundNotification());

        createNode(intent);
        startNode();

        return START_STICKY;
    }

    private void createNode(Intent intent) {
        MultiTreeMap nodeConfig;

        if (intent != null && intent.hasExtra(KEY_CONFIG)
                && intent.getSerializableExtra(KEY_CONFIG) instanceof MultiTreeMap) {
            nodeConfig = (MultiTreeMap) intent.getSerializableExtra(KEY_CONFIG);
            hasConfig = true;
        } else {
            nodeConfig = new MultiTreeMap();
            nodeConfig.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, deviceNodeName);
        }

        node = new Node(nodeConfig);
        node.setLogLevel(LoggerSimple.Level.ALL);
        if (!hasConfig) {
            Pylon localPylon = new LocalPylon();
            pylonsList.add(localPylon);
            node.addGeneralContext(localPylon.asContext());
        }

        GlobalLogWrapper.setLogStream(logsOutputStream);
        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);
        FlashManager.getInstance().getAgentsLiveData().observeForever(agentsObserver);
    }

    private void startNode() {
        if (running) {
            return;
        }

        logsOutputStream.reset();

        running = node.start();
        runningLiveData.postValue(running);

        Toast.makeText(this, "Service started",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        running = !node.stop();
        runningLiveData.postValue(running);
        FlashManager.getInstance().getAgentsLiveData().removeObserver(agentsObserver);
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
