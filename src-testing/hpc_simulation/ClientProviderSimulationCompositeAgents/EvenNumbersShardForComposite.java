package hpc_simulation.ClientProviderSimulationCompositeAgents;

import java.util.Random;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.local.LocalPylon;

public class EvenNumbersShardForComposite extends AgentShardCore {
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

    private MessagingPylonProxy pylon;
    public static final String EVEN_NUMBERS_COUNT = ProviderServices.EVEN_NUMBERS.toString();
    public static String EVEN_NUMBERS_SHARD_DESIGNATION = "Even numbers shard designation";
    public static final int MAX_LIMIT = 10;

    private String parentAgentName = "";
    private String clientName = "";
    private boolean isWaiting = true;


    protected EvenNumbersShardForComposite(AgentShardDesignation designation) {
        super(designation);
    }


    public void findEvenNumbersCount() {

        int evenNumbersCount = 0;
        int maxLimit = new Random().nextInt(MAX_LIMIT);

        for(int nr = 2; nr <= maxLimit; nr++) {
            if (isEven(nr)) {
                evenNumbersCount++;
            }
        }

		LocalPylon.SimpleLocalMessaging messagingShard = (LocalPylon.SimpleLocalMessaging) getAgent()
				.getAgentShard(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
        messagingShard.sendMessage( parentAgentName,  clientName,  EVEN_NUMBERS_COUNT + " " + evenNumbersCount);


    }

    private boolean isEven(int number) {
        if (number % 2 == 0)
            return true;
        return false;
    }


    @Override
    public void signalAgentEvent(AgentEvent event)
    {
        /* The source is the name of the parent agent. The Content */
        if(event instanceof AgentWave){
            if(((AgentWave) event).getCompleteSource().contains("User") &&
                    (((AgentWave) event).getContent().equals(EVEN_NUMBERS_COUNT))) {
                        isWaiting = false;
                        parentAgentName = ((AgentWave)event).getCompleteDestination();
                        clientName = ((AgentWave)event).getCompleteSource();
                        findEvenNumbersCount();
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



    private void printMessage(AgentEvent event) {
        System.out.println("SLAVE: " + ((AgentWave) event).getContent() + " de la "
                + ((AgentWave) event).getCompleteSource() + " la " +
                ((AgentWave) event).getCompleteDestination());
    }

}
