package PrimeNumberSimulationCompositeAgents;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.local.LocalSupport;

public class PrimeNumberCalculatorShardForComposite extends AgentShardCore {


    private int primeNumbersLimit;
    private boolean isWaiting = true;
    private String agentName;

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
    protected PrimeNumberCalculatorShardForComposite(AgentShardDesignation designation) {
        super(designation);
    }

    private MessagingPylonProxy pylon;
    public static final String PRIME_NUMBERS_COUNT = "prime numbers found";
    public static final String CALCULATOR_SHARD_DESIGNATION = "Prime number calculator shard designation";

    public void findPrimeNumbersCount() {
        waitInfoForProcessing();
        int primeNumbersCount = 0;

        for(int nr = 2; nr <= primeNumbersLimit; nr++) {
            if (isPrime(nr)) {
                primeNumbersCount++;
            }
        }
        LocalSupport.SimpleLocalMessaging messagingShard = (LocalSupport.SimpleLocalMessaging) getAgent().getAgentShard(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
        messagingShard.sendMessage(  agentName,  "Master",  Integer.toString(primeNumbersCount));
    }

    private boolean isPrime(int number) {
        for(int factor = 2; factor <= number / 2 ; factor++) {
            if (number % factor == 0)
                return false;
        }
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event)
    {
        if(event instanceof AgentWave){
            if(((AgentWave) event).getCompleteSource().equals( "Master" )) {
                //printMessage(event);
                primeNumbersLimit = Integer.parseInt(
                        ((AgentWave) event).getContent());
                isWaiting = false;
                agentName = ((AgentWave) event).getCompleteDestination();
            }
        }

    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MessagingPylonProxy))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MessagingPylonProxy) context;
        return true;
    }


    private void waitInfoForProcessing() {
        while (isWaiting) {
            ;
        }
    }


    private void printMessage(AgentEvent event) {
        System.out.println("SLAVE: " + ((AgentWave) event).getContent() + " de la "
                + ((AgentWave) event).getCompleteSource() + " la " +
                ((AgentWave) event).getCompleteDestination());
    }


}
