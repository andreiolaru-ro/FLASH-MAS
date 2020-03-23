package com.flashmas.lib;

import android.os.Handler;
import android.util.Log;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.local.LocalSupport;
import net.xqhs.flash.local.LocalSupport.SimpleLocalMessaging;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.logging.LogWrapper;

public class TestAgent extends Unit implements Agent {

    private String name;
    private AbstractMessagingShard messagingShard;
    private MessagingPylonProxy pylon;
    private boolean running = false;
    public ShardContainer proxy = new ShardContainer() {
        @Override
        public void postAgentEvent(final AgentEvent event) {
            TestAgent.this.postAgentEvent(event);
        }

        @Override
        public String getEntityName() {
            return getName();
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            // no support for shard discovery.
            return null;
        }

    };

    public TestAgent(String name) {
        this.name = name;
        setUnitName(name);
        setLoggerType(PlatformUtils.platformLogType());
    }

    private void postAgentEvent(final AgentEvent event) {
        if (!running) {
            return;
        }

        Log.d("DEBUG", event.getValue(
                AbstractMessagingShard.CONTENT_PARAMETER) + " de la "
                + event.getValue(AbstractMessagingShard.SOURCE_PARAMETER)
                + " la " + event.getValue(
                AbstractMessagingShard.DESTINATION_PARAMETER));

        li(event.getValue(
                AbstractMessagingShard.CONTENT_PARAMETER) + " de la "
                + event.getValue(AbstractMessagingShard.SOURCE_PARAMETER)
                + " la " + event.getValue(
                AbstractMessagingShard.DESTINATION_PARAMETER));
        final int message = Integer.parseInt(
                event.getValue(AbstractMessagingShard.CONTENT_PARAMETER));
        Runnable eventThread = new Runnable() {
            @Override
            public void run() {
                getMessagingShard().sendMessage(
                        event.getValue(
                                AbstractMessagingShard.DESTINATION_PARAMETER),
                        event.getValue(
                                AbstractMessagingShard.SOURCE_PARAMETER),
                        Integer.toString(
                                message + 1));
            }
        };

        new Handler().postDelayed(eventThread, 2000);
    }

    @Override
    public boolean start() {

        running = true;
        li("Agent [] started", name);
        if (name.equals("Agent 2")) {
            messagingShard.sendMessage(this.getName(), "Agent 1", "1");
        }
        return true;
    }

    @Override
    public boolean stop() {
        li("Agent [] stopped", name);
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        pylon = (MessagingPylonProxy) context;
        if (messagingShard != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return true;
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {
        pylon = null;
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Agent> asContext() {
        return proxy;
    }

    public boolean addMessagingShard(AbstractMessagingShard shard) {
        messagingShard = shard;
        shard.addContext(proxy);
        if (pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    protected AbstractMessagingShard getMessagingShard() {
        return messagingShard;
    }
}
