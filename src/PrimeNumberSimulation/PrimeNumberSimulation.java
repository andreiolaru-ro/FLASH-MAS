package PrimeNumberSimulation;


import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Override
    public void run() {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        li("Starting node [].", name);
        for(Entity<?> entity : entityOrder) {
            if(entity instanceof Agent) {
                lf("running an entity...");
                Runnable agentTask = () -> entity.run();

                pool.execute(agentTask);
            } else {
                lf("running an entity...");
                entity.run();
            }
        }
        li("Node [] is running.", name);

        pool.shutdown();
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

    public static void addContextToAgentList(LocalSupport pylon, ArrayList<PrimeNumberAgent> agentList) {

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
            agentList.get(i).addMessagingShard(new LocalSupport.SimpleLocalMessaging());
        }
    }


    public static void main(String[] args) {

        MultiTreeMap configuration = new MultiTreeMap();
        configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "testNode");
        PrimeNumbersNode node = new PrimeNumbersNode(configuration);
        LocalSupport pylon = new LocalSupport();


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
        masterAgent.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
        masterAgent.addControlSlaveAgentsShard(new ControlSlaveAgentsShard());



        node.registerAgentsInNode(masterAgent, agentList);

        node.start();
        node.run();

    }
}
