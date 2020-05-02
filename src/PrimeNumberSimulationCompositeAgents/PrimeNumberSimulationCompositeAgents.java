package PrimeNumberSimulationCompositeAgents;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PrimeNumberSimulationCompositeAgents {


    private static int SLAVE_AGENT_COUNT = 10;
    private static int MAX_THREADS = 5;

    public static ArrayList<PrimeNumberCompositeAgent> createAgentList(int agentCount) {
        ArrayList<PrimeNumberCompositeAgent> agentList = new ArrayList<>();
        for(int i = 0; i < agentCount; i++) {
            MultiTreeMap configuration = new MultiTreeMap();
            configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "Agent " + Integer.toString(i));
            agentList.add(new PrimeNumberCompositeAgent(configuration));
        }

        return agentList;
    }

    public static void addContextToAgentList(LocalSupport pylon, ArrayList<PrimeNumberCompositeAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++) {
            agentList.get(i).addContext(pylon.asContext());
        }
    }

    public static void addShardsToAgentList( ArrayList<PrimeNumberCompositeAgent> agentList) {

        for(int i = 0; i < agentList.size(); i++){
            ArrayList<AgentShardCore> shards = new ArrayList<>();
            shards.add(new PrimeNumberCalculatorShardForComposite(AgentShardDesignation.customShard(PrimeNumberCalculatorShardForComposite.CALCULATOR_SHARD_DESIGNATION)));
            shards.add(new LocalSupport.SimpleLocalMessaging());
            agentList.get(i).addShards(shards);
        }
    }

    public static void runAgents(ArrayList<PrimeNumberCompositeAgent> agents) {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        for(PrimeNumberCompositeAgent agent: agents) {
            Runnable agentTask = () -> agent.run();
            pool.execute(agentTask);
        }

        pool.shutdown();
    }


    public static void main(String[] args) {

        LocalSupport pylon = new LocalSupport();

        /* Create slave agents*/
        ArrayList<PrimeNumberCompositeAgent> agentList;
        agentList = createAgentList(SLAVE_AGENT_COUNT);

        addContextToAgentList(pylon, agentList);
        addShardsToAgentList(agentList);

        /* Create master agent */
        MultiTreeMap configuration = new MultiTreeMap();
        configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "Master");
        MasterCompositeAgent masterAgent = new MasterCompositeAgent(configuration);

        masterAgent.addContext(pylon.asContext());
        masterAgent.setSlaveAgentsCount(agentList.size());

        ArrayList<AgentShardCore> shards = new ArrayList<>();
        shards.add(new LocalSupport.SimpleLocalMessaging());
        ControlSlaveAgentShardForComposite controlShard = new ControlSlaveAgentShardForComposite(AgentShardDesignation.customShard(ControlSlaveAgentShardForComposite.CONTROL_SHARD_DESIGNATION));
        controlShard.setSlaveAgentsCounts(SLAVE_AGENT_COUNT);
        shards.add(controlShard);
        masterAgent.addShards(shards);



        //*  START EVERYTHING *//

        //* Uncomment this to start agents with run() *//
        runAgents(agentList);

        masterAgent.start();

        //* Uncomment this to start slaves with start() *//
        /*for(PrimeNumberCompositeAgent agent: agentList) {
            agent.start();
        }*/

        ArrayList a = agentList;
        //*  START POSTING EVENTS IN AGENTS *//

        /* Make master send limits to slaves {giveTasksToAgents}*/
        LocalSupport.SimpleLocalMessaging messagingShardMaster =  masterAgent.getMessagingShard();
        messagingShardMaster.sendMessage(masterAgent.getName(), masterAgent.getName(), ControlSlaveAgentShardForComposite.SEND_LIMITS);


        //* STOP EVERYTHING  *//

       ControlSlaveAgentShardForComposite contrlShard = masterAgent.getControlShard();
       while(!contrlShard.isSimulationReady()) {
           ;
          // System.out.println("while");
       }

        masterAgent.stop();
        for(PrimeNumberCompositeAgent agent : agentList) {
            agent.stop();
        }

        System.out.println("Stop " + Thread.activeCount()  + " threaduri");
    }

}
