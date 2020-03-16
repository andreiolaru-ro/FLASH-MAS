package ClientProviderSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;

import javax.swing.*;
import java.util.Random;

public class NumberMultiplesCountShard extends AgentShardCore {
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
     * @param designation - the designation of the shard, as instance of {@link AgentShardDesignation.StandardAgentShard}.
     */
    protected NumberMultiplesCountShard(AgentShardDesignation designation) {
        super(designation);
    }

    private MessagingPylonProxy pylon;
    public static final String NUMBER_MULTIPLES_COUNT = "the count of multiples found for given number";
    public static  final int MAX_LIMIT = 50;
    public static  final int MAX_NUMBER = 100;
    public static String NUMBER_MULTIPLES_SHARD_DESIGNATION = "Number multiples shard designation";

    public void findNumberMultiplesCount() {

        int number = new Random().nextInt(MAX_NUMBER);
        int maxLimit = new Random().nextInt(MAX_LIMIT);

        int numberMultiplesCount = 0;

        for(int nr = 2 * number; nr <= maxLimit; nr++) {
            if (isMultipleOfNumber(nr, number)) {
                numberMultiplesCount++;
            }
        }

        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        event.add(NUMBER_MULTIPLES_COUNT, Integer.toString(numberMultiplesCount));
        getAgent().postAgentEvent(event);

    }

    private boolean isMultipleOfNumber(int nrForCheck, int  number) {
        if  (nrForCheck % number == 0)
            return true;

        return false;
    }


    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MessagingPylonProxy))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MessagingPylonProxy) context;
        return true;
    }


}
