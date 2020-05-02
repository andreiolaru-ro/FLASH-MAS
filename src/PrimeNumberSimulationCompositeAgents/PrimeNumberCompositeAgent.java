package PrimeNumberSimulationCompositeAgents;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;

import java.util.ArrayList;

public class PrimeNumberCompositeAgent extends CompositeAgent {

    private MessagingPylonProxy pylon;

    protected PrimeNumberCompositeAgent(MultiTreeMap configuration) {
        super(configuration);
    }

    protected void addShards(ArrayList<AgentShardCore> shards) {
        for (AgentShardCore shard : shards) {
            addShard(shard);
        }
    }

    public LocalSupport.SimpleLocalMessaging getMessagingShard() {
        LocalSupport.SimpleLocalMessaging messagingShard = (LocalSupport.SimpleLocalMessaging) getShard(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
        return  messagingShard;
    }


}
