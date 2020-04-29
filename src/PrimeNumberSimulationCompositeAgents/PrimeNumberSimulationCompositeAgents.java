package PrimeNumberSimulationCompositeAgents;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;

import java.util.ArrayList;


public class PrimeNumberSimulationCompositeAgents {


    private static int SLAVE_AGENT_COUNT = 10;

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
        masterAgent.start();
        for(PrimeNumberCompositeAgent agent: agentList) {
            agent.start();
        }

        //*  START POSTING EVENTS IN AGENTS *//

        /* Make master send limits to slaves {giveTasksToAgents}*/
        LocalSupport.SimpleLocalMessaging messagingShardMaster =  masterAgent.getMessagingShard();
        messagingShardMaster.sendMessage(masterAgent.getName(), masterAgent.getName(), ControlSlaveAgentShardForComposite.SEND_LIMITS);

        /* Start agents  {findPrimeNumbersCount }*/
       for(PrimeNumberCompositeAgent agent: agentList) {
           LocalSupport.SimpleLocalMessaging messagingShardSlave =  agent.getMessagingShard();
           messagingShardSlave.sendMessage(agent.getName(), "Agent " + Integer.toString(1),  PrimeNumberCalculatorShardForComposite.START_PROCESSING);
       }


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
