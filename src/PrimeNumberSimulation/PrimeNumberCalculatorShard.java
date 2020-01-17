package PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

public class PrimeNumberCalculatorShard extends AgentShardCore {
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

    private MasterSlavePylon pylon;
    public static final String PRIME_NUMBERS_COUNT = "prime numbers found";

    protected PrimeNumberCalculatorShard() {
        super(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
    }

    protected void findPrimeNumbersCount(int maxLimit) {
        int primeNumbersCount = 0;

        for(int nr = 2; nr <= maxLimit; nr++) {
            if (isPrime(nr)) {
                primeNumbersCount++;
            }
        }

        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        event.add(PRIME_NUMBERS_COUNT, Integer.toString(primeNumbersCount));
        getAgent().postAgentEvent(event);

    }

    private boolean isPrime(int number) {
        for(int factor = 2; factor <= number / 2 ; factor++)
        {
            if (number % factor == 0)
                return false;
        }
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MasterSlavePylon))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MasterSlavePylon) context;
        //pylon.register(getAgent().getEntityName(), inbox);
        return true;
    }
}
