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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.config.Config;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;
import net.xqhs.util.logging.wrappers.GlobalLogWrapper;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.flashmas.lib.Globals.NODE_NAME;

public class NodeForegroundService extends Service {
    public static final String KEY_CONFIG = "key_config";
    public static final String TAG = NodeForegroundService.class.getSimpleName();
    private static boolean running = false;
    private static MutableLiveData<Boolean> runningLiveData = new MutableLiveData<>();
    private Node node;
    private List<Pylon> pylonsList = null;
    private String deviceNodeName = NODE_NAME;  // init deviceNodeName with default
    static ByteArrayOutputStream logsOutputStream = new ByteArrayOutputStream();
    private boolean hasConfig = false;
    private Thread nodeThread = null;

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
        DeploymentConfiguration nodeConfig;

        if (intent != null && intent.hasExtra(KEY_CONFIG)
                && intent.getSerializableExtra(KEY_CONFIG) instanceof DeploymentConfiguration) {
            nodeConfig = (DeploymentConfiguration) intent.getSerializableExtra(KEY_CONFIG);
            hasConfig = true;
        } else {
            nodeConfig = new DeploymentConfiguration();
            try {
                nodeConfig.loadConfiguration(Arrays.asList(""), false, null);
            } catch (Config.ConfigLockedException e) {
                e.printStackTrace();
            }
//            nodeConfig.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, deviceNodeName);
        }

        DeploymentConfiguration.isCentralNode = false;
        List<MultiTreeMap> allEntities = nodeConfig.getEntityList();
        List<MultiTreeMap> nodesTrees = DeploymentConfiguration.filterCategoryInContext(allEntities,
                CategoryName.NODE.s(), null);
        if(nodesTrees == null || nodesTrees.isEmpty()) { // the DeploymentConfiguration should have created at least an
            // empty node.
            Log.e(TAG, "No nodes present in config");
            return;
        }

        Log.d(TAG, nodesTrees.get(nodesTrees.size() - 1).toString());

        node = new NodeLoader().load(nodesTrees.get(nodesTrees.size() - 1), DeploymentConfiguration.filterContext(allEntities,
                nodesTrees.get(nodesTrees.size() - 1).getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE)));
        pylonsList = new LinkedList<>();
        for (Entity e: node.getEntitiesList()) {
            if (e instanceof Pylon) {
                pylonsList.add((Pylon)e);
            }
            if (e instanceof Agent) {
                FlashManager.getInstance().addAgent((Agent)e);
            }
        }

        node.setLogLevel(LoggerSimple.Level.ALL);
        GlobalLogWrapper.setLogStream(logsOutputStream);
        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);
        FlashManager.getInstance().getAgentsLiveData().observeForever(agentsObserver);

        nodeThread = new Thread() {
            @Override
            public void run() {
                running = node.start();
                runningLiveData.postValue(running);
            }
        };
    }

    private void startNode() {
        if (running) {
            return;
        }

        logsOutputStream.reset();

        nodeThread.start();

        Toast.makeText(this, "Service started",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        node.stop();
        try {
            nodeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        running = node.isRunning();
        runningLiveData.postValue(running);
        node = null;
        pylonsList = null;
        nodeThread = null;
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
