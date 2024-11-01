package hpc_simulation.ClientProviderSimulation;

import java.security.InvalidParameterException;
import java.util.HashMap;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.hpc.RunnableAgent;
import net.xqhs.flash.local.LocalPylon;

public class ProviderAgent implements RunnableAgent {


    public volatile Boolean isWaiting = true;
    private String name;
    private MessagingPylonProxy pylon;
    private HashMap<AgentShardDesignation, AgentShardCore> shards = new HashMap<>();
    private HashMap<ProviderServices, AgentShardCore> shardsByService = new HashMap<>();
    private static final long MAX_WAITING_TIME_FOR_JOB = 10000000;

    private static final String AVAILABLE = "";
    private String currentCustomer = AVAILABLE;
    private String service = "";
    private static final String supervisorName = "Supervisor";

    private Object providerLock = new Object();

    private boolean STOP = false;


    private ShardContainer masterProxy = new ShardContainer() {
        @Override
        public String getEntityName() {
            return name;
        }

        @Override
		public boolean postAgentEvent(AgentEvent event) {

            synchronized (providerLock){

                /*  Verify if there is a received message and if the message is a request */
                if(event instanceof AgentWave &&
                        eventContainsRequest(((AgentWave) event).getContent())) {

                    printMessage(event);
                    /* If the provider is not available, send a deny to the user */
                    if (!isAvailable()) {
                        declineJob(event, " { Busy } ");
						return false;
                    }
                    /* If the provider doesn't have the requested service, send a deny to the user */
                    if (!hasService(((AgentWave) event).getContent())){
                        declineJob(event, " { No service } ");
						return false;
                    }

                    /* Otherwise, notify the user that this provider will take the job and start processing*/
                    setCurrentCustomer(((AgentWave) event).getCompleteSource());
                    acceptJob(event);
                    //CALL THE NEEDED SHARD IN RUN FOR IT TO RUN ON A THREAD
                    service = ((AgentWave) event).getContent();
                    setIsWaiting(false);
                    providerLock.notify();
                }

                if(eventContainsResultFromShard(event)) {
                    setIsWaiting(true);
                    sendResult(service);
                    releaseCurrentCustomer();
                }

                if(stopSignalFromSupervisor(event)) {
                    providerLock.notify();
                    setSTOP(true);
                }
				return true;
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
        Thread thread = Thread.currentThread();
        thread.setPriority(Thread.MAX_PRIORITY);
        long last_service_time = System.nanoTime();

        synchronized (providerLock){
            while(!STOP) {

                while(getIsWaiting() && !STOP) {

                    try {
                        providerLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }

                }
                if (!service.equals("") ) {
                    startShardForService(service);
                }

            }
        }
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

	private LocalPylon.SimpleLocalMessaging getMessagingShard() {

		return (LocalPylon.SimpleLocalMessaging) shards
				.get(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
    }

    private void declineJob(AgentEvent event, String reason) {
        getMessagingShard()
                .sendMessage(
                        ((AgentWave) event).getCompleteDestination(),
                        ((AgentWave) event).getCompleteSource(),
                        "NO");
    }

    private void acceptJob(AgentEvent event) {
        getMessagingShard()
                .sendMessage(
                        ((AgentWave) event).getCompleteDestination(),
                        ((AgentWave) event).getCompleteSource(),
                        "YES");
    }

    private void sendResult(String shardResult) {
        getMessagingShard().sendMessage(getName(), currentCustomer, shardResult );
    }

    private void startShardForService(String requestedService) {
        if (service.equals("")) {
            return;
        }
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
        System.out.println("["+getName()+"] " + ((AgentWave) event).getContent() +
                " de la " + ((AgentWave) event).getCompleteSource()+ " la " +
                ((AgentWave) event).getCompleteDestination());
    }

    private synchronized void setIsWaiting(boolean value){
        isWaiting = value;
    }

    private synchronized boolean getIsWaiting(){
        return isWaiting;
    }

    private boolean stopSignalFromSupervisor(AgentEvent event) {
        if(event instanceof  AgentWave &&
                ((AgentWave) event).getCompleteSource().equals(supervisorName) &&
                ((AgentWave) event).getContent().equals("STOP")) {
                return true;

        }
        return false;
    }


    private void setSTOP(boolean value) {
        this.STOP = value;
    }

    private boolean getSTOP() {
        return STOP;
    }
}
