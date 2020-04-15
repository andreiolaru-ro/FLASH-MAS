package net.xqhs.flash.core.monitoring;

import net.xqhs.flash.core.monitoring.gui.GUIBoard;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.local.LocalPylon;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CentralMonitoringAndControlEntity implements  Entity<Pylon> {

    protected static final String	SHARD_ENDPOINT				        = "control";

    protected static final String	OTHER_SHARD_ENDPOINT				= "control";

    private MessagingShard          centralMessagingShard;

    private String                  name;

    /*
    * Graphic User Interface
    * */
    private GUIBoard                GUI;

    private static boolean          RUNNING_STATE;

    protected MonitoringNodeProxy powerfulProxy;


    // Proxy used to receive messages from outer entities; e.g. logs from agents
    public ShardContainer          proxy = new ShardContainer() {
            @Override
            public void postAgentEvent(AgentEvent event) {
                System.out.println(event.toString());
                if(event.getType().equals(AgentEvent.AgentEventType.AGENT_WAVE))
                {
                    String content = ((AgentWave) event).getContent();
                    System.out.println(" [] [] " + content);
                }
            }

            @Override
            public AgentShard getAgentShard(AgentShardDesignation designation) {
                return null;
            }

            @Override
            public String getEntityName() {
                return getName();
            }
    };

    public CentralMonitoringAndControlEntity(String name) {
        this.name = name;
    }


    @Override
    public boolean start() {
        if(centralMessagingShard == null)
            throw new IllegalStateException("No messaging shard present");
        centralMessagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        System.out.println("[" + getName() + "] starting...");
        RUNNING_STATE = true;
        return true;
    }

    public void startGUIBoard() {
        SwingUtilities.invokeLater(() -> {
            try {
                GUI = new GUIBoard(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean stop() {
        System.out.println(getName() + "## CENTRAL MONITORING ENTITY STOPPED...");
        RUNNING_STATE = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return RUNNING_STATE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        PylonProxy pylonProxy = (PylonProxy) context;
        String recommendedShard = pylonProxy
                .getRecommendedShardImplementation(
                        AgentShardDesignation.standardShard(
                                AgentShardDesignation.StandardAgentShard.MESSAGING));
        try
        {
            centralMessagingShard = (MessagingShard) PlatformUtils
                    .getClassFactory().loadClassInstance(recommendedShard, null, true);
        } catch(ClassNotFoundException
                | InstantiationException | NoSuchMethodException
                | IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        centralMessagingShard.addContext(proxy);
        return centralMessagingShard.addGeneralContext(context);
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return addContext((MessagingPylonProxy) context);
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return null;
    }

    public boolean addNodeProxy(EntityProxy<Node> proxy)
    {
        if(!(proxy instanceof MonitoringNodeProxy))
            throw new IllegalStateException("Node proxy is not of expected type");
        powerfulProxy = (MonitoringNodeProxy)proxy;
        return true;
    }


    /**
    * Requests to send a control command. This is mainly coming
     * from the GUI component.
    **/

    public boolean sendGUICommand(String entityName, String command) {
        centralMessagingShard
                .sendMessage(
                        AgentWave.makePath(getName(), SHARD_ENDPOINT),
                        AgentWave.makePath(entityName, OTHER_SHARD_ENDPOINT),
                        command);
        return true;
    }

    public boolean sendToAllAgents(String command) {
        List<String > agents = powerfulProxy.getAgentsFromOuterNodes();
        agents.addAll(powerfulProxy.getOwnAgents());
        for(String ag : agents)
        {
            centralMessagingShard
                    .sendMessage(
                            AgentWave.makePath(getName(), SHARD_ENDPOINT),
                            AgentWave.makePath(ag, OTHER_SHARD_ENDPOINT),
                            command);
        }
        return true;
    }
}
