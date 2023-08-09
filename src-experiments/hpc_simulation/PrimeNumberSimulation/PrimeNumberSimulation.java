package hpc_simulation.PrimeNumberSimulation;


import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.RunnableEntity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalPylon;

class PrimeNumbersNode extends Node
{
    public static final int MAX_THREADS = 3;
    public PrimeNumbersNode(MultiTreeMap configuration)
    {

        super(configuration);
    }

    public void registerAgentsInNode(MasterAgent master,  ArrayList<PrimeNumberAgent> agentList) {
        registerEntity("Agent", master, master.getName());
        for(Agent agent : agentList ) {
            registerEntity("Agent",agent, agent.getName() );
        }
    }

	public void run() {
		// long startTime = System.nanoTime();
		
		ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
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
		li("Node [] is running.", name);
		
		pool.shutdown();
		
		// System.out.println( "Simulation time " + (System.nanoTime() - startTime));
		
	}
}

public class PrimeNumberSimulation {

    public static ArrayList<PrimeNumberAgent> createAgentList(int agentCount) {
        ArrayList<PrimeNumberAgent> agentList = new ArrayList<>();
        for(int i = 0; i < agentCount; i++) {
            agentList.add(new PrimeNumberAgent(Integer.toString(i)));
        }

        return agentList;
    }

	public static void addContextToAgentList(LocalPylon pylon, ArrayList<PrimeNumberAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++) {
            agentList.get(i).addContext(pylon.asContext());
        }
    }

    public static void addPrimeNumberCalculatorShardToAgentList( ArrayList<PrimeNumberAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++){
            agentList.get(i).addPrimeNumbersCalculatorShard(new PrimeNumberCalculatorShard());
        }
    }

    public static void addMessagingShardToAgentList( ArrayList<PrimeNumberAgent> agentList)
    {
        for(int i = 0; i < agentList.size(); i++)
        {
			agentList.get(i).addMessagingShard(new LocalPylon.SimpleLocalMessaging());
        }
    }


    public static void main(String[] args) {

        MultiTreeMap configuration = new MultiTreeMap();
        configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "testNode");
        PrimeNumbersNode node = new PrimeNumbersNode(configuration);
		LocalPylon pylon = new LocalPylon();


        /* Create slave agents */
        ArrayList<PrimeNumberAgent> agentList;
        agentList = createAgentList(1000000);
        addContextToAgentList(pylon, agentList);
        addPrimeNumberCalculatorShardToAgentList(agentList);
        addMessagingShardToAgentList(agentList);

        /* Create master agent */
        MasterAgent masterAgent = new MasterAgent("Master");
        masterAgent.addContext(pylon.asContext());
        masterAgent.setSlaveAgentsCount(agentList.size());
		masterAgent.addMessagingShard(new LocalPylon.SimpleLocalMessaging());
        masterAgent.addControlSlaveAgentsShard(new ControlSlaveAgentsShard());



        node.registerAgentsInNode(masterAgent, agentList);

        node.start();
        node.run();

    }
}
