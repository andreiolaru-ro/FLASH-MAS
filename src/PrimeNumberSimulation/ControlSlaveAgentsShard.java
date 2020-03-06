package PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.PylonProxy;

import java.util.ArrayList;
import java.util.Random;

public class ControlSlaveAgentsShard extends AgentShardCore {
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

    private static final int PRIME_NUMBERS_LIMIT = 50;
    public static final String SIMULATION_TIME = "Simulation time";
    public static final String SIMULATION_START_TIME = "Simulation start time";
    public static final String LIMIT = "Limit";
    private MessagingPylonProxy pylon;
    private static  int startedAgentsNumber;
    private static long startTime;


    public ControlSlaveAgentsShard() {
        super(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
        startedAgentsNumber = 0;
        startTime = 0;
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

        AgentEvent event  = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        event.add(SIMULATION_START_TIME, String.valueOf(startTime));
        getAgent().postAgentEvent(event);
        /* Make all agents find number of prime numbers to a certain limit */
        for (int i = 0; i < slaveAgentsCount; i++)
        {
            int limit = new Random().nextInt(PRIME_NUMBERS_LIMIT);

            AgentEvent event2  = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
            event2.add(LIMIT, String.valueOf(limit));
            getAgent().postAgentEvent(event2);
        }



    }


    public void gatherAgentsResults() {

    }
}
