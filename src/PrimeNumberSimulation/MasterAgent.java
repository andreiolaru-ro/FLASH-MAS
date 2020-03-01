package PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.*;

import java.util.ArrayList;

public class MasterAgent implements Agent {

    private String name;
    private int slaveAgentsCount;
    private ControlSlaveAgentsShard controlShard;
    private AbstractMessagingShard  messagingShard;
    private MessagingPylonProxy pylon;
    private static long startTime;
    private ShardContainer masterProxy = new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) {

            if(event.containsKey(ControlSlaveAgentsShard.SIMULATION_START_TIME)) {
                startTime = Long.parseLong(event.get(ControlSlaveAgentsShard.SIMULATION_START_TIME));
            } else {
                /*System.out.println(event.getValue(
                        AbstractMessagingShard.CONTENT_PARAMETER) + " de la "
                        + event.getValue(AbstractMessagingShard.SOURCE_PARAMETER)
                        + " la " + event.getValue(
                        AbstractMessagingShard.DESTINATION_PARAMETER));*/
                decrement();
                if(slaveAgentsCount == 0) {
                    long elapsedTime = System.nanoTime() - startTime;
                    System.out.println("Simulation time " + elapsedTime + " " + slaveAgentsCount);
                }
            }

        }

        @Override
        public String getEntityName() {
            return getName();
        }

        public synchronized void decrement() {
            slaveAgentsCount--;
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
        pylon = (MessagingPylonProxy) context;
        if(controlShard != null)
            controlShard.addGeneralContext(pylon);
        if(messagingShard != null)
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

    public boolean addMessagingShard(AbstractMessagingShard shard)
    {
        messagingShard = shard;
        shard.addContext(masterProxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }
}
