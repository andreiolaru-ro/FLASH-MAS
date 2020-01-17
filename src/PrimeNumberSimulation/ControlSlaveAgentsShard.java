package PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

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
    private MasterSlavePylon pylon;

    protected ControlSlaveAgentsShard() {
        super(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MasterSlavePylon))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MasterSlavePylon) context;
        //pylon.register(getAgent().getEntityName(), inbox);
        return true;
    }

    public void giveTasksToAgents(ArrayList<PrimeNumberAgent> slaveAgents){

        int runningAgentsNumber = 0;
        long startTime = System.nanoTime();
        /* Make all agents find number of prime numbers to a certain limit */
        for(PrimeNumberAgent agent : slaveAgents) {
            int limit = new Random().nextInt(PRIME_NUMBERS_LIMIT);
            agent.setPrimeNumbersLimit(limit);
            agent.startProcessingPrimeNumbers();
            runningAgentsNumber++;
        }

        /* Gather agents' results */
        //Trebuie sa ma asigur ca s-a terminat operatia
        //asta nue buna; in postAgentEvent din PrimeNumberAgent
        //ar trebui sa fac un semnal care sa avertizeze masterul
        //ca s-a terminat un agent
        while(runningAgentsNumber > 0){
            for(PrimeNumberAgent agent : slaveAgents) {
                if(agent.getPrimeNumbersCount() != 0){
                    runningAgentsNumber--;
                }
            }
        }
        long elapsedTime = System.nanoTime() - startTime;

        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        event.add(SIMULATION_TIME,Long.toString(elapsedTime));
        getAgent().postAgentEvent(event);

    }
}
