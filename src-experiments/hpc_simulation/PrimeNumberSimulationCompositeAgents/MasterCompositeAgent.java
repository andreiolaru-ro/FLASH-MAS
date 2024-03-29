package hpc_simulation.PrimeNumberSimulationCompositeAgents;

import java.util.ArrayList;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalPylon;

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

	public LocalPylon.SimpleLocalMessaging getMessagingShard() {
		LocalPylon.SimpleLocalMessaging messagingShard = (LocalPylon.SimpleLocalMessaging) getShard(
				AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
        return  messagingShard;
    }

    public ControlSlaveAgentShardForComposite getControlShard() {
        ControlSlaveAgentShardForComposite controlShard = (ControlSlaveAgentShardForComposite) getShard(AgentShardDesignation.customShard(ControlSlaveAgentShardForComposite.CONTROL_SHARD_DESIGNATION));
        return  controlShard;
    }

}
