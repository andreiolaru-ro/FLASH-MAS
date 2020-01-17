package PrimeNumberSimulation;


import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PrimeNumbersNode extends Node
{
    public static final int MAX_THREADS = 3;
    public PrimeNumbersNode(String name)
    {
        super(name);
    }

    public void registerAgentsInNode(MasterAgent master,  ArrayList<PrimeNumberAgent> agentList) {
        registerEntity("Agent", master, master.getName());
        for(Agent agent : agentList ) {
            registerEntity("Agent",agent, agent.getName() );
        }
    }

    /*Astea ar trebui mutate in Node. Metoda porneste toate entitatile inregistrate
     * intr-un nod. Pune task-urile agentilor intr-un thread pool.
     * Nu prea stiu cum ar functiona asta daca ei asteapta mesaje de la alti agenti
     * adormiti. POate doar daca ceva asteapta prea mult, sa intre in waiting.*/
    @Override
    public boolean start() {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        li("Starting node [].", name);
        for(Entity<?> entity : entityOrder) {
            if(!(entity instanceof Agent)) {
                lf("starting an entity...");
                if (entity.start())
                    lf("entity started successfully.");
                else
                    le("failed to start entity.");
            } else {
                lf("starting an entity...");
                Runnable agentTask = () -> entity.start();

                pool.execute(agentTask);
            }
        }
        li("Node [] started.", name);

        pool.shutdown();
        return true;
    }
}

public class PrimeNumberSimulation {

    public static ArrayList<PrimeNumberAgent> createAgentList(int agentCount) {
        ArrayList<PrimeNumberAgent> agentList = new ArrayList<>();
        for(int i = 0; i < agentCount; i++) {
            agentList.add(new PrimeNumberAgent(String.valueOf(i)));
        }

        return agentList;
    }

    public static void addContextToAgentList(MasterSlavePylon pylon, ArrayList<PrimeNumberAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++) {
            agentList.get(i).addContext(pylon.asContext());
        }
    }

    public static void addPrimeNumberCalculatorShardToAgentList( ArrayList<PrimeNumberAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++){
            agentList.get(i).addPrimeNumbersCalculatorShard(new PrimeNumberCalculatorShard());
        }
    }




    public static void main(String[] args) {

        PrimeNumbersNode node = new PrimeNumbersNode("testNode");
        MasterSlavePylon pylon = new MasterSlavePylon();



        /* Create slave agents */
        ArrayList<PrimeNumberAgent> agentList;
        agentList = createAgentList(2000000);
        addContextToAgentList(pylon, agentList);
        addPrimeNumberCalculatorShardToAgentList(agentList);

        /* Create master agent */
        MasterAgent masterAgent = new MasterAgent("Master");
        masterAgent.addContext(pylon.asContext());
        masterAgent.addControlSlaveAgentsShard(new ControlSlaveAgentsShard());
        masterAgent.setSlaveAgents(agentList);


        node.registerAgentsInNode(masterAgent, agentList);


        //startAgents(agentList);
        node.start();

    }
}
