package com.flashmas.app.agents;

import android.util.Log;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;

public class TestAgent implements Agent {

    private String name;
    private AbstractMessagingShard messagingShard;
    private MessagingPylonProxy pylon;

    private ShardContainer proxy = new ShardContainer() {
        @Override
        public void postAgentEvent(final AgentEvent event) {
            Log.i("TestAgent", event.getValue(
                    AbstractMessagingShard.CONTENT_PARAMETER) + " de la "
                    + event.getValue(AbstractMessagingShard.SOURCE_PARAMETER)
                    + " la " + event.getValue(
                    AbstractMessagingShard.DESTINATION_PARAMETER));
            final int message = Integer.parseInt(
                    event.getValue(AbstractMessagingShard.CONTENT_PARAMETER));
            if (message < 5) {
                Thread eventThread = new Thread() {
                    @Override
                    public void run() {
                        getMessagingShard()
                                .sendMessage(
                                        event.getValue(
                                                AbstractMessagingShard.DESTINATION_PARAMETER),
                                        event.getValue(
                                                AbstractMessagingShard.SOURCE_PARAMETER),
                                        Integer.toString(
                                                message + 1));
                    }
                };
                eventThread.run();
            }
        }

        @Override
        public String getEntityName() {
            return getName();
        }

    };

    public TestAgent(String name) {
        this.name = name;
    }

    @Override
    public boolean start() {
        if (name.equals("Two")) {
            messagingShard.sendMessage(this.getName(), "One", "1");
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return true;
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
