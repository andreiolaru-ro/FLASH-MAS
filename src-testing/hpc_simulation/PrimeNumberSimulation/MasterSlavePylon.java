package hpc_simulation.PrimeNumberSimulation;

import java.util.ArrayList;

import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;

public class MasterSlavePylon extends DefaultPylonImplementation {

    private static ArrayList<PrimeNumberAgent> slaveAgents;
    private static MasterAgent masterAgent;
    private static final long  MAX_ITERATIONS = 1000000000;
    public MasterPylonProxy  proxy = new MasterPylonProxy();

    public MasterSlavePylon()
    {
        super();
        slaveAgents = new ArrayList<>();
        masterAgent = new MasterAgent("");
    }

    public void setSlaveAgents(ArrayList<PrimeNumberAgent> slaveAgents)
    {
        this.slaveAgents = slaveAgents;
    }

    public void setMasterAgent(MasterAgent masterAgent)
    {
        this.masterAgent = masterAgent;
    }

    public static int signalSlaves( ArrayList<Integer> limits)
    {
        int startedAgentsNumber = 0;
        for(PrimeNumberAgent agent : slaveAgents)
        {
            agent.setPrimeNumbersLimit(limits.get(slaveAgents.indexOf(agent)));
            agent.startProcessingPrimeNumbers();
            startedAgentsNumber++;
        }
        return  startedAgentsNumber;
    }

    public static boolean retrieveInfoFromSlaves(int startedAgentsNumber)
    {
        int iterationNumber = 0;
        while(startedAgentsNumber > 0 && iterationNumber < MAX_ITERATIONS){
            for(PrimeNumberAgent agent : slaveAgents) {
                if(agent.getPrimeNumbersCount() != 0){
                    startedAgentsNumber--;
                }
            }
            iterationNumber++;
        }
        if (iterationNumber < MAX_ITERATIONS)
        {
            return true;
        } else {
            return false;
        }
    }



    @Override
    public EntityProxy<Pylon> asContext()
    {
        return proxy;
    }

    public static class MasterPylonProxy implements PylonProxy {

        @Override
        public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
            return null;
        }

        @Override
        public String getEntityName() {
            return null;
        }

        public int signalSlaves(ArrayList<Integer> limits)
        {
            return MasterSlavePylon.signalSlaves(limits);
        }
        public boolean retrieveInfoFromSlaves(int startedAgentsNumber)
        {
            return MasterSlavePylon.retrieveInfoFromSlaves(startedAgentsNumber);
        }
    }
}
