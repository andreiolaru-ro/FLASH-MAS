package PrimeNumberSimulationCompositeAgents;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

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


    @Override
    public void run() {
        PrimeNumberCalculatorShardForComposite calculatorShard = (PrimeNumberCalculatorShardForComposite) getShard(AgentShardDesignation.customShard(PrimeNumberCalculatorShardForComposite.CALCULATOR_SHARD_DESIGNATION));
        calculatorShard.findPrimeNumbersCount();
    }



}
