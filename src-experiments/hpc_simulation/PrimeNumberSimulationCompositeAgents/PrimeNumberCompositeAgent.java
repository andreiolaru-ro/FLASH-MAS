package hpc_simulation.PrimeNumberSimulationCompositeAgents;

import java.util.ArrayList;

import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalPylon;

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

	public LocalPylon.SimpleLocalMessaging getMessagingShard() {
		LocalPylon.SimpleLocalMessaging messagingShard = (LocalPylon.SimpleLocalMessaging) getShard(
				AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
        return  messagingShard;
    }


}
