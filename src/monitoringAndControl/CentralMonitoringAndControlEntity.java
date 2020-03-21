package monitoringAndControl;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;

public class CentralMonitoringAndControlEntity implements Entity<Node> {

    // CentralMonitoringShard within this special entity
    private AbstractMessagingShard centralMonitoringShard;

    private String                 name;

    /*
    * Proxy to a specific node.
    * TODO:
    *  Later we might need a structure to store multiple proxies.
    *  Each proxy for each node in the network.
    *  CentralMonitoringAndControlEntity instance must have access to all of them.
    * */
    private MonitoringNodeProxy    nodeProxy;


    public CentralMonitoringAndControlEntity(String name) {
        this.name = name;
        addMonitoringShard(new CentralMonitoringShard());
    }

    // Proxy used to receive messages from outer entities; e.g. logs from agents
    // TODO: you have to figure out which shard is the receiver from the container
    public ShardContainer          proxy = new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) {
        //TODO: Here is the message from CentralMonitoringShard.
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }

        @Override
        public String getEntityName() {
            return null;
        }
    };

    public boolean addMonitoringShard(AbstractMessagingShard shard)
    {
        centralMonitoringShard = shard;
        shard.addContext(proxy);
        if(nodeProxy != null)
            centralMonitoringShard.addGeneralContext(nodeProxy);
        return true;
    }


    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Node> context) {
        return false;
    }

    /*
    * Keep the MonitoringNodeProxy reference from the main node.
    * */
    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        nodeProxy = (MonitoringNodeProxy) context;
        if(centralMonitoringShard != null) {
            centralMonitoringShard.addGeneralContext(nodeProxy);
        }
        return true;
    }

    @Override
    public boolean removeContext(EntityProxy<Node> context) {
        return false;
    }

    @Override
    public <C extends Entity<Node>> EntityProxy<C> asContext() {
        return null;
    }
}
