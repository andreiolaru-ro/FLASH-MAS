package ClientProviderSimulationCompositeAgents;

import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

public class UserRequestShardForComposite extends AgentShardCore {
    /**
     * The constructor assigns the designation to the shard.
     * <p>
     * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
     * parent agent or on other shards, as when the shard is created, the {@link AgentShardCore#parentAgent} member is
     * <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes by calling
     * the method {@link AgentShardCore#parentChangeNotifier}.
     * <p>
     * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
     * {@link #shardInitializer()} method.
     *
     * @param designation - the designation of the shard, as instance of {@link StandardAgentShard}.
     */
    protected UserRequestShardForComposite(AgentShardDesignation designation) {
        super(designation);
    }
}
