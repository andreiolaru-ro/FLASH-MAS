package ClientProviderSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.local.LocalSupport;

import javax.swing.*;
import javax.swing.tree.ExpandVetoException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;

public class ProviderAgent implements Agent {


    private boolean isWaiting = true;
    private String name;
    private MessagingPylonProxy pylon;
    private HashMap<AgentShardDesignation, AgentShardCore> shards = new HashMap<>();
    private HashMap<ProviderServices, AgentShardCore> shardsByService = new HashMap<>();
    private static final long MAX_WAITING_TIME_FOR_JOB = 10000000;

    private String AVAILABLE = "";
    private String currentCustomer = AVAILABLE;
    private String service = "";

    private ShardContainer masterProxy = new ShardContainer() {
        @Override
        public String getEntityName() {
            return name;
        }

        @Override
        public void postAgentEvent(AgentEvent event) {

            /*  Verify if there is a received message and if the message is a request */
            if(event.containsKey(AbstractMessagingShard.CONTENT_PARAMETER) &&
                eventContainsRequest(event.get(AbstractMessagingShard.CONTENT_PARAMETER))) {

                //printMessage(event);
                /* If the provider is not available, send a deny to the user */
                if (!isAvailable()) {
                    //System.out.println("Ocupat");
                    declineJob(event);
                    return;
                }
                    /* If the provider doesn't have the requested service, send a deny to the user */
                if (!hasService(event.get(AbstractMessagingShard.CONTENT_PARAMETER))){
                    declineJob(event);
                    //System.out.println("Nu am serviciul");
                    return;
                }

                /* Otherwise, notify the user that this provider will take the job and start processing*/
                    setCurrentCustomer(event.get(AbstractMessagingShard.SOURCE_PARAMETER));
                    acceptJob(event);
                    //CALL THE NEEDED SHARD IN RUN FOR IT TO RUN ON A THREAD
                    //startShardForService(event.get(AbstractMessagingShard.CONTENT_PARAMETER));
                    service = event.get(AbstractMessagingShard.CONTENT_PARAMETER);
                    setIsWaiting(false);
                    startShardForService(service);

            }

            if(eventContainsResultFromShard(event)) {
                setIsWaiting(true);
                sendResult(extractServiceFromShardResult(event));
                releaseCurrentCustomer();
            }
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }
    };

    public ProviderAgent(String name) {
        this.name = name;
    }

    public AgentShard getAgentShard(AgentShardDesignation designation)
    {
        try {
            return shards.get(designation);
        } catch (Exception e) {
            throw new InvalidParameterException(
                    "There is no shard with the designation [" + designation + "]");
        }
    }

    public ProviderAgent addShard(AgentShardCore shard) {

        if(shard == null)
            throw new InvalidParameterException("Shard is null");
        if(hasShard(shard.getShardDesignation()))
            throw new InvalidParameterException(
                    "Cannot add multiple shards for designation [" + shard.getShardDesignation() + "]");
        shards.put(shard.getShardDesignation(), shard);
        shard.addContext(this.asContext());
        if(pylon != null)
            shard.addGeneralContext(pylon);
        return this;

    }

    public ProviderAgent addShard(ProviderServices service, AgentShardCore shard) {

        if(shard == null)
            throw new InvalidParameterException("Shard is null");
        if(hasShard(shard.getShardDesignation()))
            throw new InvalidParameterException(
                    "Cannot add multiple shards for designation [" + shard.getShardDesignation() + "]");
        shards.put(shard.getShardDesignation(), shard);
        shardsByService.put(service, shard);
        shard.addContext(this.asContext());
        if(pylon != null)
            shard.addGeneralContext(pylon);
        return this;

    }

    protected boolean hasShard(AgentShardDesignation designation)
    {
        return shards.containsKey(designation);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void run() {
       /* long last_service_time = 0;
        while(true) {
            if(!isWaiting) {
                startShardForService(service);
                last_service_time = System.nanoTime();
            }


            if( System.nanoTime() - last_service_time  > MAX_WAITING_TIME_FOR_JOB ) {
                break;
            }
        }*/
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
        for(AgentShardCore shard : shards.values()) {
            shard.addGeneralContext(pylon);
        }
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
        return masterProxy;
    }

    private void setCurrentCustomer(String customer) {
        this.currentCustomer = customer;
    }

    private String getCurrentCustomer(){
        return this.currentCustomer;
    }

    private void releaseCurrentCustomer(){
        this.currentCustomer = AVAILABLE;
    }

    private boolean isAvailable(){
        if(getCurrentCustomer().equals(AVAILABLE))
            return true;
        return false;
    }

    private boolean hasService(String request) {
        ProviderServices requestedService = ProviderServices.valueOf(request);

        for(ProviderServices service : shardsByService.keySet()) {
            if(requestedService.equals(service)) {
                return true;
            }
        }
        return false;
    }

    private boolean eventContainsRequest(String request) {

        if(request.equals(ProviderServices.EVEN_NUMBERS.toString()))
            return true;
        if(request.equals(ProviderServices.FIBONACCI.toString()))
            return true;
        if(request.equals(ProviderServices.NUMBER_MULTIPLES.toString()))
            return true;
        if(request.equals(ProviderServices.ODD_NUMBERS.toString()))
            return true;
        if(request.equals(ProviderServices.QUADRATIC_EQUATIONS.toString()))
            return true;

        return false;

    }

    private String extractServiceFromShardResult(AgentEvent event) {
        if(event.containsKey(EvenNumbersShard.EVEN_NUMBERS_COUNT))
            return ProviderServices.EVEN_NUMBERS.toString();
        if(event.containsKey(OddNumbersShard.ODD_NUMBERS_COUNT))
            return ProviderServices.ODD_NUMBERS.toString();
        if(event.containsKey(FibonacciShard.FIBONACCI_VALUE))
            return ProviderServices.FIBONACCI.toString();
        if(event.containsKey(NumberMultiplesCountShard.NUMBER_MULTIPLES_COUNT))
            return ProviderServices.NUMBER_MULTIPLES.toString();
        if(event.containsKey(QuadraticEquationSolverShard.EQUATION_ROOTS))
            return ProviderServices.QUADRATIC_EQUATIONS.toString();

        return "";
    }

    private boolean eventContainsResultFromShard(AgentEvent event) {

        if(event.containsKey(EvenNumbersShard.EVEN_NUMBERS_COUNT))
            return true;
        if(event.containsKey(OddNumbersShard.ODD_NUMBERS_COUNT))
            return true;
        if(event.containsKey(FibonacciShard.FIBONACCI_VALUE))
            return true;
        if(event.containsKey(NumberMultiplesCountShard.NUMBER_MULTIPLES_COUNT))
            return true;
        if(event.containsKey(QuadraticEquationSolverShard.EQUATION_ROOTS))
            return true;

        return false;
    }

    private LocalSupport.SimpleLocalMessaging getMessagingShard() {

        return (LocalSupport.SimpleLocalMessaging) shards.get(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
    }

    private void declineJob(AgentEvent event) {
        getMessagingShard()
                .sendMessage(
                        event.getValue(
                                AbstractMessagingShard.DESTINATION_PARAMETER),
                        event.getValue(
                                AbstractMessagingShard.SOURCE_PARAMETER),
                        "NO");
    }

    private void acceptJob(AgentEvent event) {
        getMessagingShard()
                .sendMessage(
                        event.getValue(
                                AbstractMessagingShard.DESTINATION_PARAMETER),
                        event.getValue(
                                AbstractMessagingShard.SOURCE_PARAMETER),
                        "YES");
    }

    private void sendResult(String shardResult) {
        getMessagingShard().sendMessage(getName(), currentCustomer, shardResult );
    }

    private void startShardForService(String requestedService) {
        ProviderServices service = ProviderServices.valueOf(requestedService);
        AgentShardCore shard = shardsByService.get(service);

        if(shard instanceof EvenNumbersShard) {
            ((EvenNumbersShard) shard).findEvenNumbersCount();
            return;
        }
        if(shard instanceof FibonacciShard) {
            ((FibonacciShard) shard).startFibonacci();
            return;
        }
        if(shard instanceof NumberMultiplesCountShard) {
            ((NumberMultiplesCountShard) shard).findNumberMultiplesCount();
            return;
        }
        if(shard instanceof OddNumbersShard) {
            ((OddNumbersShard) shard).findOddNumbersCount();
            return;
        }
        if(shard instanceof QuadraticEquationSolverShard) {
            ((QuadraticEquationSolverShard) shard).findQuadraticEqationRoots();
            return;
        }

    }

    private void printMessage(AgentEvent event) {
        System.out.println("[" + getName() +"] "  + event.get(AbstractMessagingShard.CONTENT_PARAMETER) +
                " de la " + event.get(AbstractMessagingShard.SOURCE_PARAMETER )+ " la " +
                event.get(AbstractMessagingShard.DESTINATION_PARAMETER));
    }

    private synchronized void setIsWaiting(boolean value){
        isWaiting = value;
    }
}
