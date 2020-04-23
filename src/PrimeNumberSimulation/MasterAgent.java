package PrimeNumberSimulation;

import java.util.ArrayList;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;

public class MasterAgent implements Agent {

    private String name;
    private int slaveAgentsCount;
    private ControlSlaveAgentsShard controlShard;
    private AbstractMessagingShard  messagingShard;
    private MessagingPylonProxy pylon;
    private static long startTime;
    private ArrayList<Integer> limits = new ArrayList<>();
    private ShardContainer masterProxy = new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) {

            if(event.containsKey(ControlSlaveAgentsShard.SIMULATION_START_TIME)) {
                startTime = Long.parseLong(event.get(ControlSlaveAgentsShard.SIMULATION_START_TIME));
                return;
            }

            if(event.containsKey(ControlSlaveAgentsShard.LIMIT)) {
                int limit = Integer.parseInt(event.get(ControlSlaveAgentsShard.LIMIT));
                limits.add(limit);

                if(limits.size() == slaveAgentsCount) {
                    for(int i = 0; i < limits.size(); i++) {
                        getMessagingShard().sendMessage("Master", Integer.toString(i), Integer.toString(limits.get(i)));
                    }
                }
                return;
            }

            decrement();
            //printMessage(event);
            if(slaveAgentsCount == 0) {
                long elapsedTime = System.nanoTime() - startTime;
                System.out.println("Simulation time " + elapsedTime + " " + slaveAgentsCount);
            }


        }

        @Override
        public String getEntityName() {
            return getName();
        }

        public synchronized void decrement() {
            slaveAgentsCount--;
        }
													
        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation)
        {
            if(designation.equals(
                    StandardAgentShard.MESSAGING.toAgentShardDesignation()))
                return getMessagingShard();
            return null;
        }
    };

    public MasterAgent(String name) {
        this.name = name;
    }

	AbstractMessagingShard getMessagingShard()
    {
        return this.messagingShard;
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
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
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

    private void printMessage(AgentEvent event) {
        System.out.println("["+getName()+"] " + ((AgentWave) event).getContent() +
                " de la " + ((AgentWave) event).getCompleteSource()+ " la " +
                ((AgentWave) event).getCompleteDestination());
    }
}
