package ClientProviderSimulationCompositeAgents;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;

public class SupervisorCompositeAgent extends CompositeAgent {


    private int usersCount = 0;
    private int providersCount = 0;
    private int messagesFromUsers = 0;
    private long startTime = 0;

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
    public SupervisorCompositeAgent(MultiTreeMap configuration) {
        super(configuration);
    }

    protected void addShards(ArrayList<AgentShardCore> shards) {
        for(AgentShardCore shard: shards) {
            addShard(shard);
        }
    }

    public int getUsersCount(){
        return usersCount;
    }

    public void setUsersCount(int usersCount) {
        this.usersCount  = usersCount;
    }

    public int getProvidersCount(){
        return  providersCount;
    }

    public void setProvidersCount(int providersCount){
        this.providersCount = providersCount;
    }
}
