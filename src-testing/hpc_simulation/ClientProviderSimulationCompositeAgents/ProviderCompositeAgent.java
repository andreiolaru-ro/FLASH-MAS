package hpc_simulation.ClientProviderSimulationCompositeAgents;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;

public class ProviderCompositeAgent extends CompositeAgent {
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
    public ProviderCompositeAgent(MultiTreeMap configuration) {
        super(configuration);
    }

    protected void addShards(ArrayList<AgentShardCore> shards) {
        for (AgentShardCore shard : shards) {
            addShard(shard);
        }
    }
}
