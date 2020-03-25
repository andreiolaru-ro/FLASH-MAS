package monitoringAndControl;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;

public class CentralMonitoringAndControlEntity implements Entity<Node> {

    // Messaging shard to receive monitoring messages.
    private AbstractMessagingShard centralMessagingShard;

    private String                 name              = null;

    /*
    * Proxy to a specific node.
    * TODO: Solve the connection with different/multiple nodes.
    * */
    private MonitoringNodeProxy    nodeProxy;


    /*
    * Graphic User Interface
    * */
    private MainBoard GUI;

    public CentralMonitoringAndControlEntity(String name) {
        this.name = name;
        addMessagingShard(new CentralMessagingShard());
        /*
        * TODO: The arg should disappear because the ARGS for loading
        *  might be taken as GUI input.
        * */
        GUI = new MainBoard(null);
    }

    // Proxy used to receive messages from outer entities; e.g. logs from agents
    public ShardContainer          proxy = new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) {
        //TODO: Here is the message from CentralMonitoringShard when calling receive()
               //The parent of CentralMonitoringShard will be this ShardContainer - proxy.

            //Aici vor fi logurile primite inapoi prin shardul de monitorizare.
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

    public boolean addMessagingShard(AbstractMessagingShard shard)
    {
        centralMessagingShard = shard;
        shard.addContext(proxy);
        if(nodeProxy != null)
            centralMessagingShard.addGeneralContext(nodeProxy);
        return true;
    }


    @Override
    public boolean start() {
        //GUI will actually appear when the monitoringEntity will start.
        javax.swing.SwingUtilities.invokeLater(() -> GUI.createAndShowGUI());
        System.out.println("CentralMonitoringAndControl started successfully!");
        return true;
    }

    @Override
    public boolean stop() {
        return true;
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
        if(centralMessagingShard != null) {
            centralMessagingShard.addGeneralContext(nodeProxy);
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

    /**
    * Requests to the entity to send a start control command. This is mainly coming
     * from the GUI component.
    **/

    public boolean sendGUIStartCommand(String entityName) {
        // destination: entityName
        // TODO: send a (WAVE?) message through the pylon
        //  to agent's monitoring shard;
        //  monitoring shard will post a STOP event;
        return true;
    }

    public boolean sendGUIStopCommand(String entityName) {
        // destination: entityName
        // TODO: send a (WAVE?) message through the pylon
        //  to agent's monitoring shard;
        //  monitoring shard will post a STOP event;
        return true;
    }
}
