package ClientProviderSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;

public class ProviderAgent implements Agent {


    private String name;
    private MessagingPylonProxy pylon;
    private HashMap<AgentShardDesignation,AgentShardCore> shards = new HashMap<>();
    private ShardContainer masterProxy = new ShardContainer() {
        @Override
        public String getEntityName() {
            return name;
        }

        @Override
        public void postAgentEvent(AgentEvent event) {

        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }
    };

    public ProviderAgent(String name) {
        this.name = name;
    }


    public ProviderAgent addShard(AgentShardCore shard) {

        if(shard == null)
            throw new InvalidParameterException("Shard is null");
        if(hasShard(shard.getShardDesignation()))
            throw new InvalidParameterException(
                    "Cannot add multiple shards for designation [" + shard.getShardDesignation() + "]");
        shards.put(shard.getShardDesignation(), shard);
        shard.addContext(this.asContext());
        if(pylon != null)
            shard.addGeneralContext(pylon);
        return this;

    }

    protected boolean hasShard(AgentShardDesignation designation)
    {
        return shards.containsKey(designation);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void run() {

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
        for(AgentShardCore shard : shards.values()) {
            shard.addGeneralContext(pylon);
        }
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
    public  EntityProxy<Agent> asContext() {
        return masterProxy;
    }
}
