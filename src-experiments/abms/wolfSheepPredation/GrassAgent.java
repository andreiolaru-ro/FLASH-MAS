package abms.wolfSheepPredation;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;

public class GrassAgent extends CompositeAgent implements ShardContainer, EntityProxy<Agent> {

    private volatile boolean grown = true;

    public GrassAgent() {
        addShard(new GrassBehaviorShard());
    }

    public boolean isGrown() {
        return grown;
    }

    public void setGrown(boolean grown) {
        this.grown = grown;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Agent> asContext() {
        return this;
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        return super.postAgentEvent(event);
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        return shards.get(designation);
    }

    @Override
    public String getEntityName() {
        return getName() != null ? getName() : "Grass";
    }
}
