package ClientProviderSimulationCompositeAgents;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.local.LocalSupport;

import java.util.Random;

public class NumberMultiplesCountShardForComposite extends AgentShardCore {
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
    protected NumberMultiplesCountShardForComposite(AgentShardDesignation designation) {
        super(designation);
    }

    private MessagingPylonProxy pylon;
    public static final String NUMBER_MULTIPLES_COUNT  = ProviderServices.NUMBER_MULTIPLES.toString();
    public static  final int MAX_LIMIT = 10;
    public static  final int MAX_NUMBER = 100;

    private String parentAgentName = "";
    private String clientName = "";
    private boolean isWaiting = true;


    public void findNumberMultiplesCount() {

        int number = 0;
        while(number == 0)
            number = new Random().nextInt(MAX_NUMBER);
        int maxLimit = new Random().nextInt(MAX_LIMIT);

        int numberMultiplesCount = 0;

        for(int nr = 2 * number; nr <= maxLimit; nr++) {
            if (isMultipleOfNumber(nr, number)) {
                numberMultiplesCount++;
            }
        }

        LocalSupport.SimpleLocalMessaging messagingShard = (LocalSupport.SimpleLocalMessaging) getAgent().getAgentShard(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
        messagingShard.sendMessage( parentAgentName,  clientName,  NUMBER_MULTIPLES_COUNT + " " + numberMultiplesCount);

    }

    private boolean isMultipleOfNumber(int nrForCheck, int  number) {
        if  (nrForCheck % number == 0)
            return true;

        return false;
    }



    @Override
    public void signalAgentEvent(AgentEvent event)
    {
        /* The source is the name of the parent agent. The Content */
        if(event instanceof AgentWave){
            if(((AgentWave) event).getCompleteSource().contains("User") &&
                    (((AgentWave) event).getContent().equals(NUMBER_MULTIPLES_COUNT))) {
                isWaiting = false;
                parentAgentName = ((AgentWave)event).getCompleteDestination();
                clientName = ((AgentWave)event).getCompleteSource();
                findNumberMultiplesCount();
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
