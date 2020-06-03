package PrimeNumberSimulationCompositeAgents;

import PrimeNumberSimulation.MasterAgent;
import PrimeNumberSimulation.PrimeNumberAgent;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.composite.ScheduledAgent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduledNode extends Node {
    /**
     * Creates a new {@link Node} instance.
     *
     * @param nodeConfiguration the configuration of the node. Can be <code>null</code>.
     */
    public ScheduledNode(MultiTreeMap nodeConfiguration) {
        super(nodeConfiguration);
    }

    public static final int MAX_THREADS = 3;


    public void registerAgentsInNode(MasterAgent master, ArrayList<PrimeNumberAgent> agentList) {
        registerEntity("Agent", master, master.getName());
        for(Agent agent : agentList ) {
            registerEntity("Agent",agent, agent.getName() );
        }
    }

    @Override
    public void run() {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        ArrayList<ScheduledAgent> transientAgents = new ArrayList<>();
        HashMap<ScheduledAgent, Long> agentsRunningTime = new HashMap<>();

        li("Starting node [].", name);
        for(Entity<?> entity : entityOrder) {
            if(entity instanceof Agent) {
                lf("running an entity...");

                ScheduledAgent agent = (ScheduledAgent) entity;
                agentsRunningTime.put(agent, (long) 0);

            } else {
                lf("running an entity...");
                entity.run();
            }
        }


        while(!agentsRunningTime.isEmpty() && !transientAgents.isEmpty()) {


            ScheduledAgent[] agents = new ScheduledAgent[agentsRunningTime.keySet().size()] ;
            agentsRunningTime.keySet().toArray(agents);

            for(ScheduledAgent agent: agentsRunningTime.keySet() ) {

                if(agentsRunningTime.get(agent) != 0) {
                    Runnable agentTask = () -> agent.run();
                    pool.execute(agentTask);
                    agentsRunningTime.put(agent, System.nanoTime());
                }
            }

            for(ScheduledAgent transientAgent: transientAgents) {
                agentsRunningTime.put(transientAgent, (long)0);
                transientAgents.remove(transientAgent); //S-ar putea sa fie erori la rulare
            }


            for(ScheduledAgent agent: agentsRunningTime.keySet()) {

                if(System.nanoTime() - agent.getStartedTime() >= agent.getRunningTime()) {
                    if(!agent.isStopped()){
                        agent.stop(); // in loc de isReady, verific aici daca era deja in stop
                        agent.toggleTransient();
                        transientAgents.add(agent);
                    }

                }
            }
        }
        li("Node [] is running.", name);

        pool.shutdown();
    }
}
