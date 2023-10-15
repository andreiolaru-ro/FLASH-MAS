package hpc_simulation.PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.hpc.RunnableAgent;

public class PrimeNumberAgent implements RunnableAgent {

    private String name;
    private PrimeNumberCalculatorShard calculatorShard;
    private AbstractMessagingShard messagingShard;
    private MessagingPylonProxy pylon;
    private int primeNumbersCount;
    private boolean isWaitng;
    private int primeNumbersLimit;
    public ShardContainer			primeNumberProxy	= new ShardContainer() {
        @Override
        public boolean postAgentEvent(AgentEvent event)
        {
            if(event.containsKey(PrimeNumberCalculatorShard.PRIME_NUMBERS_COUNT)) {
                primeNumbersCount = Integer.parseInt(event.get(PrimeNumberCalculatorShard.PRIME_NUMBERS_COUNT));
                getMessagingShard().sendMessage(  name, "Master",    Integer.toString(primeNumbersCount));
            } else {
                //printMessage(event);
                primeNumbersLimit = Integer.parseInt(
                        ((AgentWave) event).getContent());
                isWaitng = false;
            }
            return true;
        }

        @Override
        public String getEntityName()
        {
            return getName();
        }
																
        @Override
        public AgentShard getAgentShard(
                AgentShardDesignation designation)
        {
            if(designation.equals(StandardAgentShard.MESSAGING
                    .toAgentShardDesignation()))
                return getMessagingShard();
            return null;
        }

    };

	AbstractMessagingShard getMessagingShard()
    {
        return this.messagingShard;
    }


    public PrimeNumberAgent(String name)  {
        this.name = name;
        this.isWaitng = true;
        this.primeNumbersCount = 0;
        this.primeNumbersLimit = 0;
    }


    @Override
    public boolean start() {
        while(isWaitng) {
            ;
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void run() {
        calculatorShard.findPrimeNumbersCount(primeNumbersLimit);
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        pylon = (MessagingPylonProxy) context;
        if(calculatorShard != null)
            calculatorShard.addGeneralContext(pylon);
        if(messagingShard != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return true;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {

        pylon = null;
        return true;
    }

    @Override
    public  EntityProxy<Agent> asContext() {
        return primeNumberProxy;
    }

    public boolean addPrimeNumbersCalculatorShard(PrimeNumberCalculatorShard shard) {
        calculatorShard = shard;
        shard.addContext(primeNumberProxy);
        if(pylon != null)
            calculatorShard.addGeneralContext(pylon);
        return true;
    }

    public void startProcessingPrimeNumbers(){
        isWaitng = false;
    }

    public void setPrimeNumbersLimit(int limit){
        primeNumbersLimit = limit;
    }

    public int getPrimeNumbersCount(){
        return primeNumbersCount;
    }

    public boolean addMessagingShard(AbstractMessagingShard shard)
    {
        messagingShard = shard;
        shard.addContext(primeNumberProxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    private void printMessage(AgentEvent event) {
        System.out.println("["+getName()+"] " + ((AgentWave) event).getContent() +
                " de la " + ((AgentWave) event).getCompleteSource()+ " la " +
                ((AgentWave) event).getCompleteDestination());
    }

}
