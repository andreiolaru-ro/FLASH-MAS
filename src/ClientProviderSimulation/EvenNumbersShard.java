package ClientProviderSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import java.util.Random;

public class EvenNumbersShard extends AgentShardCore {
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
    protected EvenNumbersShard(AgentShardDesignation designation) {
        super(designation);
    }

    private MessagingPylonProxy pylon;
    public static final String EVEN_NUMBERS_COUNT = "even numbers found";
    public static final int MAX_LIMIT = 10;
    public static String EVEN_NUMBERS_SHARD_DESIGNATION = "Even numbers shard designation";

    public void findEvenNumbersCount() {
        int evenNumbersCount = 0;
        int maxLimit = new Random().nextInt(MAX_LIMIT);

        for(int nr = 2; nr <= maxLimit; nr++) {
            if (isEven(nr)) {
                evenNumbersCount++;
            }
        }

        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        event.add(EVEN_NUMBERS_COUNT, Integer.toString(evenNumbersCount));
        getAgent().postAgentEvent(event);

    }

    private boolean isEven(int number) {
        if (number % 2 == 0)
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
