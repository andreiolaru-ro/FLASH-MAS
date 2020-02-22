package PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;

import java.util.ArrayList;

public class MasterAgent implements Agent {

    private String name;
    private int slaveAgentsCount;
    private ControlSlaveAgentsShard controlShard;
    private PylonProxy pylon;
    private ShardContainer masterProxy = new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) {
            System.out.println("Simulation time " + event.get(ControlSlaveAgentsShard.SIMULATION_TIME));
        }

        @Override
        public String getEntityName() {
            return null;
        }
    };

    public MasterAgent(String name) {
        this.name = name;
    }

    @Override
    public boolean start() {
        controlShard.giveTasksToAgents(slaveAgentsCount);
        return true;
    }

    @Override
    public void run() {
        controlShard.gatherAgentsResults();
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
        pylon = (PylonProxy) context;
        if(controlShard != null)
            controlShard.addGeneralContext(pylon);
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

    @Override
    public EntityProxy<Agent> asContext() {
        return masterProxy;
    }


    public void setSlaveAgentsCount(int slaveAgentsCount)
    {
        this.slaveAgentsCount = slaveAgentsCount;
    }

    public boolean addControlSlaveAgentsShard(ControlSlaveAgentsShard shard) {
        controlShard = shard;
        shard.addContext(masterProxy);
        if(pylon != null)
            controlShard.addGeneralContext(pylon);
        return true;
    }
}
