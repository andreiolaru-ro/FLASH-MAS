package ClientProviderSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import java.util.Random;

public class FibonacciShard extends AgentShardCore {
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
    protected FibonacciShard(AgentShardDesignation designation) {
        super(designation);
    }

    private MessagingPylonProxy pylon;
    public static final String FIBONACCI_VALUE = "last number in the fibonacci series";
    public static final int MAX_LIMIT = 50;
    public static String FIBONACCI_SHARD_DESIGNATION = "Fibonacci shard designation";

    public void startFibonacci() {
        int maxLimit = new Random().nextInt(MAX_LIMIT);

        int[] f = new int [maxLimit+2];
        f[0] = 0;
        f[1] = 1;

        for(int i = 2; i <= maxLimit; i++) {
            f[i] = f[i - 1] + f[i - 2];
        }

        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        event.add(FIBONACCI_VALUE, Integer.toString(f[maxLimit]));
        getAgent().postAgentEvent(event);

    }


    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MessagingPylonProxy))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MessagingPylonProxy) context;
        return true;
    }
}
