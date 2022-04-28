package stefania.ClientsProviders.no;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;

import static stefania.ClientsProviders.no.Constants.*;

public class SupervisorAgent implements Agent {


    private String name = "";
    private MessagingPylonProxy pylon;
    private AsynchronousMPIMessaging messagingShard;

    private int usersCount = 0;
    private int providersCount = 0;
    private int messagesFromUsers = 0;
    private long startTime = 0;
    private boolean isWaiting = true;

    Object supervisorLock  = new Object();


    public SupervisorAgent(String name) {
        this.name = name;
    }

    private ShardContainer masterProxy = new ShardContainer() {

        @Override
        public String getEntityName() {
            return getName();
        }

        @Override
        public void postAgentEvent(AgentEvent event) {
            if(messageFromUser(event)) {
                messagesFromUsers++;
                synchronized (supervisorLock) {
                    if(getUsersCount() == messagesFromUsers){
                        System.out.println(getName() + "knows all users done");
                        setIsWaiting(false);
    //                    supervisorLock.notify();
                        System.out.println("Simulation time: " + (System.nanoTime() - startTime));

    //                    for(int providerIndex = 0; providerIndex < providersCount; providerIndex++) {
    //                        sendStopMessageToProvider(providerIndex);
    //                    }
                        for (int providerIndex = USERS_COUNT + 1; providerIndex <= USERS_COUNT + PROVIDER_COUNT; providerIndex++) {
                            sendStopMessageToProvider(providerIndex);
                        }

                        messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
                    }
                }

            }
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }
    };

    @Override
    public boolean start() {
        setStartTime(System.nanoTime());
        messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
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

    private AbstractMessagingShard getMessagingShard(){
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
        getMessagingShard().sendMessage(getName(), "Provider " + providerIndex, "STOP" );
    }

    private boolean messageFromUser(AgentEvent event) {
//        if(event instanceof AgentWave &&
//                ((AgentWave) event).getCompleteSource().contains("User")){

        if (event instanceof AgentWave) {
            int source = Integer.parseInt(((AgentWave) event).getCompleteSource());
            if (source > 0 && source <= USERS_COUNT)
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

    private void setIsWaiting(boolean value) {
        this.isWaiting = value;
    }

    private  boolean getIsWaiting(){
        return isWaiting;
    }
}
