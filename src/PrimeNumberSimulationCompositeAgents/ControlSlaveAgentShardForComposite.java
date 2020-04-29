package PrimeNumberSimulationCompositeAgents;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.local.LocalSupport;

import java.util.ArrayList;
import java.util.Random;

public class ControlSlaveAgentShardForComposite extends AgentShardCore {

    private static final int PRIME_NUMBERS_LIMIT = 50;
    private MessagingPylonProxy pylon;
    private static long startTime = 0;
    private int slaveAgentsCount = 0;

    public static String CONTROL_SHARD_DESIGNATION = "Control slave agents shard designation";
    public static final String SEND_LIMITS = "Send limits";
    public static final String GATHER_RESULTS = "Gather Results";

    private boolean simulationReady = false;

    /**
     * The constructor assigns the designation to the shard.
     * <p>
     * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
     * parent agent or on other shards, as when the shard is created, the  member is
     * <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes by calling
     * the method {@link AgentShardCore#parentChangeNotifier}.
     * <p>
     * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
     * {@link #shardInitializer()} method.
     *
     * @param designation - the designation of the shard, as instance of {@link AgentShardDesignation.StandardAgentShard}.
     */
    protected ControlSlaveAgentShardForComposite(AgentShardDesignation designation) {
        super(designation);
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MessagingPylonProxy))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MessagingPylonProxy) context;
        return true;
    }

    public void giveTasksToAgents(int slaveAgentsCount) {
        startTime = System.nanoTime();

        /* Make all agents find number of prime numbers to a certain limit */
        for (int i = 0; i < slaveAgentsCount; i++) {
            int limit = new Random().nextInt(PRIME_NUMBERS_LIMIT);
            LocalSupport.SimpleLocalMessaging messagingShard = (LocalSupport.SimpleLocalMessaging) getAgent().getAgentShard(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
            messagingShard.sendMessage("Master", "Agent " + Integer.toString(i), Integer.toString(limit));
        }

    }


    @Override
    public void signalAgentEvent(AgentEvent event) {
        if(event instanceof AgentWave){

            String content = ((AgentWave) event).getContent();
            switch (content) {
                case SEND_LIMITS:
                    giveTasksToAgents(slaveAgentsCount);
                    break;
                case GATHER_RESULTS:
                    gatherAgentsResults();
                    break;
                default:
                    //printMessage(event);
                    decrementSlaveAgentCount();
                    if (slaveAgentsCount == 0) {
                        long elapsedTime = System.nanoTime() - startTime;
                        System.out.println("Simulation time " + elapsedTime + " " + slaveAgentsCount);
                        setSimulationReady(true);
                    }
                    break;
            }

        }
    }


    public synchronized void decrementSlaveAgentCount() {
        slaveAgentsCount--;
    }

    public void gatherAgentsResults() {
    }

    public void setSlaveAgentsCounts(int slaveAgentsCount) {
        this.slaveAgentsCount = slaveAgentsCount;
    }

    private void printMessage(AgentEvent event) {
        System.out.println("MASTER: " + ((AgentWave) event).getContent() + " de la "
                + ((AgentWave) event).getCompleteSource() + " la " +
                ((AgentWave) event).getCompleteDestination());
    }

    public boolean isSimulationReady (){
        return simulationReady;
    }

    public  void setSimulationReady(boolean value) {
        simulationReady = value;
    }


}
