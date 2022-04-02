package stefania.ClientProvider;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

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
import net.xqhs.flash.local.LocalPylon.SimpleLocalMessaging;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;

public class ProviderAgent implements Agent {


    public volatile Boolean isWaiting = true;
    private String name;
    private MessagingPylonProxy pylon;
    private AsynchronousMPIMessaging messagingShard;
    private HashMap<AgentShardDesignation, AgentShardCore> shards = new HashMap<>();
    private HashMap<ProviderServices, AgentShardCore> shardsByService = new HashMap<>();
    private static final long MAX_WAITING_TIME_FOR_JOB = 10000000;

    private static final String AVAILABLE = "";
    private String currentCustomer = AVAILABLE;
    private String service = "";
    private static final String supervisorName = "Supervisor";

    private Object providerLock = new Object();
//    private static LinkedBlockingQueue<AgentWave> messageQueue;
    private static LinkedBlockingQueue<AgentWave> requestQueue;
    private static LinkedBlockingQueue<AgentWave> responseQueue;

    private boolean STOP = false;


    private ShardContainer masterProxy = new ShardContainer() {
        @Override
        public String getEntityName() {
            return name;
        }

        @Override
        public void postAgentEvent(AgentEvent event) {
//            AgentWave wave = (AgentWave) event.getObject(KEY);
            if (event instanceof AgentWave) {
                AgentWave wave = (AgentWave) event;
                if (eventContainsRequest(wave.getContent()) || stopSignalFromSupervisor(wave)) {
                    System.out.println(getName() + " received request for " + wave.getContent());
                    synchronized (requestQueue) {
                        try {
                            requestQueue.put(wave);
                            requestQueue.notify();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (eventContainsResultFromShard(wave)) {
                    System.out.println(getName() + " received response for " + wave.getContent());
                    synchronized (responseQueue) {
                        try {
                            responseQueue.put(wave);
                            responseQueue.notify();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
//                } else if (stopSignalFromSupervisor(wave)) {
//                    System.out.println(getName() + " received stop signal");
//                    STOP = true;
////                    setSTOP(true);
//                }
//                synchronized (messageQueue) {
//                    try {
//                        messageQueue.put(wave);
//                        messageQueue.notify();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        }

//        @Override
//        public void postAgentEvent(AgentEvent event) {
//
//            synchronized (providerLock){
//
//                /*  Verify if there is a received message and if the message is a request */
//                if(event instanceof AgentWave &&
//                        eventContainsRequest(((AgentWave) event).getContent())) {
//
//                    printMessage(event);
//                    /* If the provider is not available, send a deny to the user */
//                    if (!isAvailable()) {
//                        declineJob(event, " { Busy } ");
//                        return;
//                    }
//                    /* If the provider doesn't have the requested service, send a deny to the user */
//                    if (!hasService(((AgentWave) event).getContent())){
//                        declineJob(event, " { No service } ");
//                        return;
//                    }
//
//                    /* Otherwise, notify the user that this provider will take the job and start processing*/
//                    setCurrentCustomer(((AgentWave) event).getCompleteSource());
//                    acceptJob(event);
//                    //CALL THE NEEDED SHARD IN RUN FOR IT TO RUN ON A THREAD
//                    service = ((AgentWave) event).getContent();
//                    setIsWaiting(false);
//                    providerLock.notify();
//                }
//
//                if(eventContainsResultFromShard(event)) {
//                    setIsWaiting(true);
//                    sendResult(service);
//                    releaseCurrentCustomer();
//                }
//
//                if(stopSignalFromSupervisor(event)) {
//                    providerLock.notify();
//                    setSTOP(true);
//                }
//            }
//        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }
    };

    public ProviderAgent(String name) {
        this.name = name;
//        messageQueue = new LinkedBlockingQueue<>();
        requestQueue = new LinkedBlockingQueue<>();
        responseQueue = new LinkedBlockingQueue<>();
    }

    public static AgentWave getMessage(LinkedBlockingQueue<AgentWave> queue) {
        AgentWave wave = null;

        synchronized(queue)
        {
            if(queue.isEmpty())
                try
                {
                    queue.wait();
                } catch(InterruptedException e)
                {
                    // do nothing
                }
            if(!queue.isEmpty())
                wave = queue.poll();
        }

        return wave;
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

    public boolean addMessagingShard(AsynchronousMPIMessaging shard)
    {
        messagingShard = shard;
        shard.addContext(masterProxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    protected boolean hasShard(AgentShardDesignation designation)
    {
        return shards.containsKey(designation);
    }

    @Override
    public boolean start() {

        messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));

//        synchronized (providerLock) {
        while (true) {
//            while (getIsWaiting() && !STOP) { }
//            while(getIsWaiting() && !STOP) {

            System.out.println(getName() + " waiting to get message!!");
            AgentWave reqWave = getMessage(requestQueue);
            System.out.println(getName() + " got msg from q " + reqWave.getContent());

            if (stopSignalFromSupervisor(reqWave)) {
                break;
            }

//            if (eventContainsRequest(wave.getContent())) {
//                printMessage(wave);
                /* If the provider is not available, send a deny to the user */
            if (!isAvailable()) {
                System.out.println(getName() + " declined the job - avail");
                declineJob(reqWave, " { Busy } ");
                continue;
            }
                /* If the provider doesn't have the requested service, send a deny to the user */
            if (!hasService(reqWave.getContent())) {
                System.out.println(getName() + " declined the job - service");
                declineJob(reqWave, " { No service } ");
                continue;
            }

                /* Otherwise, notify the user that this provider will take the job and start processing*/
            setCurrentCustomer(reqWave.getCompleteSource());
            acceptJob(reqWave);
            System.out.println(getName() + " accepted the job");
                //CALL THE NEEDED SHARD IN RUN FOR IT TO RUN ON A THREAD
            service = reqWave.getContent();

                // compute the request (optional, on a thread)
            if (!service.equals("") ) {
                System.out.println(getName() + " starting the shard for " + service);
                startShardForService(service);
            }

            AgentWave respWave = getMessage(responseQueue);
            sendResult(service);
            releaseCurrentCustomer();

//                setIsWaiting(false);
//                providerLock.notify();
//            } else if (eventContainsResultFromShard(wave)) {
//                setIsWaiting(true);
//                sendResult(service);
//                releaseCurrentCustomer();
//            } else if (stopSignalFromSupervisor(wave)) {
////                providerLock.notify();
//                setSTOP(true);
//            }
//            }
        }
//        }

        System.out.println(getName() + " CLOSING!!!");
        messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

//    @Override
//    public void run() {
//        Thread thread = Thread.currentThread();
//        thread.setPriority(Thread.MAX_PRIORITY);
//        long last_service_time = System.nanoTime();
//
//        synchronized (providerLock){
//            while(!STOP) {
//
//                while(getIsWaiting() && !STOP) {
//
//                    try {
//                        providerLock.wait();
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        e.printStackTrace();
//                    }
//
//                }
//                if (!service.equals("") ) {
//                    startShardForService(service);
//                }
//
//            }
//        }
//    }

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

//    private boolean eventContainsResultFromShard(AgentEvent event) {
//
//        if(event.containsKey(EvenNumbersShard.EVEN_NUMBERS_COUNT))
//            return true;
//        if(event.containsKey(OddNumbersShard.ODD_NUMBERS_COUNT))
//            return true;
//        if(event.containsKey(FibonacciShard.FIBONACCI_VALUE))
//            return true;
//        if(event.containsKey(NumberMultiplesCountShard.NUMBER_MULTIPLES_COUNT))
//            return true;
//        if(event.containsKey(QuadraticEquationSolverShard.EQUATION_ROOTS))
//            return true;
//
//        return false;
//    }

    private boolean eventContainsResultFromShard(AgentWave event) {

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

	private SimpleLocalMessaging getMessagingShard() {

		return (SimpleLocalMessaging) shards
				.get(AgentShardDesignation.StandardAgentShard.MESSAGING.toAgentShardDesignation());
    }

    private void declineJob(AgentWave wave, String reason) {
        messagingShard
                .sendMessage(
                        wave.getCompleteDestination(),
                        wave.getCompleteSource(),
                        "NO");
    }

    private void acceptJob(AgentWave wave) {
        messagingShard
                .sendMessage(
                        wave.getCompleteDestination(),
                        wave.getCompleteSource(),
                        "YES");
    }

    private void sendResult(String shardResult) {
        messagingShard.sendMessage(getName(), currentCustomer, shardResult );
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

    private void printMessage(AgentWave wave) {
        System.out.println("["+getName()+"] " + wave.getContent() +
                " de la " + wave.getCompleteSource()+ " la " +
                wave.getCompleteDestination());
    }

    private synchronized void setIsWaiting(boolean value){
        isWaiting = value;
    }

    private synchronized boolean getIsWaiting(){
        return isWaiting;
    }

    private boolean stopSignalFromSupervisor(AgentWave wave) {
        if(wave.getContent().equals("STOP")) {
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
