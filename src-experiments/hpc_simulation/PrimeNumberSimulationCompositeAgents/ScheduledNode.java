package hpc_simulation.PrimeNumberSimulationCompositeAgents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hpc_simulation.PrimeNumberSimulation.MasterAgent;
import hpc_simulation.PrimeNumberSimulation.PrimeNumberAgent;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.RunnableEntity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.hpc.ScheduledCompositeAgent;

public class ScheduledNode extends Node {

    public static final int MAX_THREADS = 3;


    public void registerAgentsInNode(MasterAgent master, ArrayList<PrimeNumberAgent> agentList) {
        registerEntity("Agent", master, master.getName());
        for(Agent agent : agentList ) {
            registerEntity("Agent",agent, agent.getName() );
        }
    }

	// @Override
    public void run() {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        ArrayList<ScheduledCompositeAgent> transientAgents = new ArrayList<>();
        HashMap<ScheduledCompositeAgent, Long> agentsRunningTime = new HashMap<>();

        li("Starting node [].", name);
        for(Entity<?> entity : entityOrder) {
			if(entity instanceof RunnableEntity)
				if(entity instanceof Agent) {
					lf("running an entity...");
					Runnable agentTask = () -> ((RunnableEntity) entity).run();
					
					pool.execute(agentTask);
				}
				else {
					lf("running an entity...");
					((RunnableEntity) entity).run();
				}
			else
				entity.start();
		}


        while(!agentsRunningTime.isEmpty() && !transientAgents.isEmpty()) {


            ScheduledCompositeAgent[] agents = new ScheduledCompositeAgent[agentsRunningTime.keySet().size()] ;
            agentsRunningTime.keySet().toArray(agents);

            for(ScheduledCompositeAgent agent: agentsRunningTime.keySet() ) {

                if(agentsRunningTime.get(agent) != 0) {
                    Runnable agentTask = () -> agent.run();
                    pool.execute(agentTask);
                    agentsRunningTime.put(agent, System.nanoTime());
                }
            }

            for(ScheduledCompositeAgent transientAgent: transientAgents) {
                agentsRunningTime.put(transientAgent, (long)0);
                transientAgents.remove(transientAgent); //S-ar putea sa fie erori la rulare
            }


            for(ScheduledCompositeAgent agent: agentsRunningTime.keySet()) {

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
