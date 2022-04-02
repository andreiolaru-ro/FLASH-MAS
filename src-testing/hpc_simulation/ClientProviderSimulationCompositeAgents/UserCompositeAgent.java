package hpc_simulation.ClientProviderSimulationCompositeAgents;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.HashMap;

public class UserCompositeAgent extends CompositeAgent {

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

    private HashMap<ProviderServices, Boolean> unplacedRequests = new HashMap<ProviderServices, Boolean>();
    private HashMap<ProviderServices, Boolean> solvedRequests = new HashMap<ProviderServices, Boolean>();
    private int initialRequestsCount = 0;
    private static final String supervisorName = "Supervisor";
    private int requestsCount  = 0;

    public UserCompositeAgent(MultiTreeMap configuration) {
        super(configuration);
    }


    protected void addShards(ArrayList<AgentShardCore> shards) {
        for (AgentShardCore shard : shards) {
            addShard(shard);
        }
    }

    public void addRequest(ProviderServices request) {
        unplacedRequests.put(request, false);
        requestsCount++;
    }






}
