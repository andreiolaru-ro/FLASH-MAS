package PrimeNumberSimulationCompositeAgents;

import PrimeNumberSimulation.ControlSlaveAgentsShard;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;

public class MasterCompositeAgent extends CompositeAgent {

    private int slaveAgentsCount;


    /**
     * Constructor for {@link CompositeAgent} instances.
     * <p>
     * The configuration is used to extract the name of the agent from it (as the value associated with the
     * {@link DeploymentConfiguration#NAME_ATTRIBUTE_NAME} name).
     * <p>
     * Although the name may be null, it is strongly recommended that the agent is given a (unique) name, even one that
     * is automatically generated.
     *
     * @param configuration - the configuration, from which the name of the agent will be taken.
     */
    public MasterCompositeAgent(MultiTreeMap configuration) {
        super(configuration);
    }

    protected void addShards(ArrayList<AgentShardCore> shards) {
        for(AgentShardCore shard: shards) {
            addShard(shard);
        }
    }

    public void setSlaveAgentsCount(int slaveAgentsCount) {
        this.slaveAgentsCount = slaveAgentsCount;
    }

    @Override
    public boolean start() {
        super.start();
        ControlSlaveAgentShardForComposite controlShard = (ControlSlaveAgentShardForComposite) getShard(AgentShardDesignation.customShard(ControlSlaveAgentShardForComposite.CONTROL_SHARD_DESIGNATION));
        controlShard.giveTasksToAgents(slaveAgentsCount);
        return true;
    }

    @Override
    public void run() {
        super.run();
        ControlSlaveAgentShardForComposite controlShard = (ControlSlaveAgentShardForComposite) getShard(AgentShardDesignation.customShard(ControlSlaveAgentShardForComposite.CONTROL_SHARD_DESIGNATION));
        controlShard.gatherAgentsResults();
    }




}
