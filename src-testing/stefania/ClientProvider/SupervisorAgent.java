package stefania.ClientProvider;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
//import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;

import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;
import static stefania.ClientProvider.Constants.*;

public class SupervisorAgent implements Agent {


    private String name = "";
    private MessagingPylonProxy pylon;
    private AsynchronousMPIMessaging messagingShard;
    private static LinkedBlockingQueue<AgentWave> messageQueue;

    private int usersCount = 0;
    private int providersCount = 0;
    private int messagesFromUsers = 0;
    private long startTime = 0;
//    private boolean isWaiting = true;

    Object supervisorLock  = new Object();


    public SupervisorAgent(String name) {
        this.name = name;
        messageQueue = new LinkedBlockingQueue<>();
    }

    private ShardContainer masterProxy = new ShardContainer() {

        @Override
        public String getEntityName() {
            return getName();
        }

//        @Override
//        public void postAgentEvent(AgentEvent event) {
//            if(messageFromUser(event)) {
//                messagesFromUsers++;
//                synchronized (supervisorLock) {
//                    if(getUsersCount() == messagesFromUsers){
//                        System.out.println(getName() + "knows all users done");
//                        setIsWaiting(false);
//                        supervisorLock.notify();
//                        System.out.println("Simulation time: " + (System.nanoTime() - startTime));
//                    }
//                }
//
//            }
//        }

        @Override
        public void postAgentEvent(AgentEvent event) {
//            AgentWave wave = (AgentWave) event.getObject(KEY);
            if (event instanceof AgentWave) {
                AgentWave wave = (AgentWave) event;
                System.out.println("SUPERVISOR GOT THE MESSAGE");
                synchronized (messageQueue) {
                    try {
                        messageQueue.put(wave);
                        messageQueue.notify();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }
    };

    public static AgentWave getMessage() {
        AgentWave wave = null;

        synchronized(messageQueue)
        {
            if(messageQueue.isEmpty())
                try
                {
                    messageQueue.wait();
                } catch(InterruptedException e)
                {
                    // do nothing
                }
            if(!messageQueue.isEmpty())
                wave = messageQueue.poll();
        }

        return wave;
    }

    @Override
    public boolean start() {
        setStartTime(System.nanoTime());
        messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));

        while (getUsersCount() != messagesFromUsers) {
            AgentWave wave = getMessage();

            if (messageFromUser(wave)) {
                messagesFromUsers++;
            }
        }

        System.out.println(getName() + " knows all users done");
//        setIsWaiting(false);
        System.out.println("Simulation time: " + (System.nanoTime() - startTime));

        for(int providerIndex = USERS_COUNT + 1; providerIndex <= USERS_COUNT + PROVIDER_COUNT; providerIndex++) {
            sendStopMessageToProvider(providerIndex);
        }

        messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

//    @Override
//    public void run() {
//
//        synchronized (supervisorLock) {
//            while(getIsWaiting()){
//                try {
//                    supervisorLock.wait();
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    e.printStackTrace();
//                }
//            }
//        }
//        System.out.println("Supervisor a iesit din wait in run()");
//        for(int providerIndex = 0; providerIndex < providersCount; providerIndex++) {
//            sendStopMessageToProvider(providerIndex);
//        }
//
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
    public EntityProxy<Agent> asContext() {
        return  masterProxy;
    }

    private AsynchronousMPIMessaging getMessagingShard(){
        return  messagingShard;
    }

    public int getUsersCount(){
        return usersCount;
    }

    public void setUsersCount(int usersCount) {
        this.usersCount  = usersCount;
    }

    public int getProvidersCount(){
        return  providersCount;
    }

    public void setProvidersCount(int providersCount){
        this.providersCount = providersCount;
    }

    private void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    private void  sendStopMessageToProvider(int providerIndex){
        messagingShard.sendMessage(getName(), "" + providerIndex, "STOP" );
    }

    private boolean messageFromUser(AgentWave wave) {
        int source = Integer.parseInt(wave.getCompleteSource());

        if (source > 0 && source <= USERS_COUNT) {
            return true;
        }
        return false;
    }

    public void addMessagingShard(AsynchronousMPIMessaging shard) {
        messagingShard = shard;
        shard.addContext(masterProxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
    }

//    private void setIsWaiting(boolean value) {
//        this.isWaiting = value;
//    }
//
//    private  boolean getIsWaiting(){
//        return isWaiting;
//    }
}
