package PrimeNumberSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;

public class PrimeNumberAgent implements Agent{

    private String name;
    private PrimeNumberCalculatorShard calculatorShard;
    private PylonProxy pylon;
    private int primeNumbersCount;
    private boolean isWaitng;
    private int primeNumbersLimit;

    private ShardContainer primeNumberProxy = new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) {
            primeNumbersCount = Integer.parseInt(event.get(PrimeNumberCalculatorShard.PRIME_NUMBERS_COUNT));
            //System.out.println("Agent" + name + " " + primeNumbersCount);
            //Trebuie sa ma asigur ca s-a terminat operatia
            //asta nue buna; in postAgentEvent din PrimeNumberAgent
            //ar trebui sa fac un semnal care sa avertizeze masterul
            //ca s-a terminat un agent
        }

        @Override
        public String getEntityName() {
            return null;
        }
    };

    public PrimeNumberAgent(String name)  {
        this.name = name;
        this.isWaitng = true;
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
        pylon = (PylonProxy) context;
        if(calculatorShard != null)
            calculatorShard.addGeneralContext(pylon);
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

}
