package PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.*;
import net.xqhs.flash.local.LocalSupport;

public class PrimeNumberAgent implements Agent{

    private String name;
    private PrimeNumberCalculatorShard calculatorShard;
    private AbstractMessagingShard messagingShard;
    private MessagingPylonProxy pylon;
    private int primeNumbersCount;
    private boolean isWaitng;
    private int primeNumbersLimit;
    public ShardContainer			primeNumberProxy	= new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event)
        {
            if(event.containsKey(PrimeNumberCalculatorShard.PRIME_NUMBERS_COUNT)) {
                primeNumbersCount = Integer.parseInt(event.get(PrimeNumberCalculatorShard.PRIME_NUMBERS_COUNT));
                getMessagingShard().sendMessage(  "Master", name,    Integer.toString(primeNumbersCount));


            } else {
                /*System.out.println("SLAVE: " + event.getValue(
                        AbstractMessagingShard.CONTENT_PARAMETER) + " de la "
                        + event.getValue(AbstractMessagingShard.SOURCE_PARAMETER)
                        + " la " + event.getValue(
                        AbstractMessagingShard.DESTINATION_PARAMETER));*/
                primeNumbersLimit = Integer.parseInt(
                        event.getValue(AbstractMessagingShard.CONTENT_PARAMETER));
                isWaitng = false;
            }
        }

        @Override
        public String getEntityName()
        {
            return getName();
        }

    };

    private AbstractMessagingShard getMessagingShard()
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

}
