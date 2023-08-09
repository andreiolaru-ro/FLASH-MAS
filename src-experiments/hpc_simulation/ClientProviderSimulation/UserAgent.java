package hpc_simulation.ClientProviderSimulation;

import java.util.HashMap;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.hpc.RunnableAgent;


public class UserAgent implements RunnableAgent {

    private HashMap<ProviderServices, Boolean> unplacedRequests = new HashMap<ProviderServices, Boolean>();
    private HashMap<ProviderServices, Boolean> solvedRequests = new HashMap<ProviderServices, Boolean>();
    private int requestsCount = 0;
    private int solvedRequestsCount = 0;
    private int initialRequestsCount = 0;
    private static final String supervisorName = "Supervisor";

    private String name;
    private MessagingPylonProxy pylon;
    private AbstractMessagingShard messagingShard;
    private UserRequestShard userRequestShard;
    private static String EMPTY = "";
    private String answer = EMPTY;
    private Object userLock = new Object();


    private ShardContainer masterProxy = new ShardContainer() {

        @Override
        public String getEntityName() {
            return name;
        }

        @Override
        public void postAgentEvent(AgentEvent event) {

            if(event instanceof AgentWave) {
                printMessage(event);
                /* Verifies if the message represents the acceptance or denial of a request placement */
                synchronized (userLock){
                    if(((AgentWave) event).getContent().equals("YES") ||
                            ((AgentWave) event).getContent().contains("NO")) {
                        setAnswer(((AgentWave) event).getContent());
                        userLock.notify();
                     } else {
                    /* If the message is not about an accepted or denied request, then it's
                    about the result of a request  */
                        solvedRequestsCount++;
                        ProviderServices request = ProviderServices.valueOf(((AgentWave) event).getContent());
                        setSolvedRequests(request, true);
                        //System.out.println(getName() + " solved requests " + getSolvedRequestsCount() + " "
                          //      + solvedRequestsCount + " initial requests num " + initialRequestsCount);

                        if(getSolvedRequestsCount() == getRequestCount()) {
                            /*  Notify the supervisor that you are done */
                            notifySupervisorRequestsComplted();
                        }
                    }
                }

                /* Ulterior as putea adauga la stringul content si rezultatul, pe langa numele serviciului
                 * indeplinit.
                  * Ar trebui sa gasesc o solutie, diferita de HM, ca toate serviciile sa fie marcate
                  * ca indeplinite sau nu*/
            }

        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }
    };


    public UserAgent(String name){
        this.name = name;

    }

    @Override
    public boolean start() {
        //printInitialState();
        initialRequestsCount = unplacedRequests.size();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void run() {

        Thread thread = Thread.currentThread();
        thread.setPriority(Thread.NORM_PRIORITY);

        synchronized (unplacedRequests){
            while(countLeftRequests() > 0 ) {
                for( ProviderServices request : unplacedRequests.keySet() ) {
                    if(!unplacedRequests.get(request)) {
                        makeRequest(request);
                    }
                }
            }
        }
        //finalAnounce();
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
        if(userRequestShard != null)
            userRequestShard.addGeneralContext(pylon);
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
        return  masterProxy;
    }

    public boolean addUserRequestShard(UserRequestShard shard) {
        userRequestShard = shard;
        shard.addContext(masterProxy);
        if(pylon != null)
            userRequestShard.addGeneralContext(pylon);
        return true;
    }

    public boolean addMessagingShard(AbstractMessagingShard shard)
    {
        messagingShard = shard;
        shard.addContext(masterProxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    public void addRequest(ProviderServices request) {
        unplacedRequests.put(request, false);
        requestsCount++;
    }

    /*Broadcast but be sure that JUST ONE agent takes the service*/
    private void makeRequest(ProviderServices request){

        synchronized (this){
            for(int i = 0; i< ClientProviderSimulation.PROVIDER_COUNT; i++) {

                getMessagingShard().sendMessage(getName(), "Provider " + i, request.toString());
                waitForAnswear(); //Lista globala cu providerii care mi-au mai ramas de intrebat
                if(getAnswer().equals("YES")) {
                    removeRequest(request);
                    setSolvedRequests(request, false);
                    setAnswer(EMPTY);
                    break;
                }
                /* Search for another provider. So the previous answer is no longer needed */
                if(getAnswer().contains("NO")) {
                    setAnswer(EMPTY);
                }
            }
        }

    }

    private void setAnswer(String content) {
        answer = content;
    }

    private String getAnswer(){
        return answer;
    }

    private void waitForAnswear() {
        synchronized (userLock) {
            while(getAnswer().equals(EMPTY)){
                try {
                    userLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }
    }

    private void printMessage(AgentEvent event) {
        System.out.println("["+getName()+"] " + ((AgentWave) event).getContent() +
                " de la " + ((AgentWave) event).getCompleteSource()+ " la " +
                ((AgentWave) event).getCompleteDestination());
    }

    private AbstractMessagingShard getMessagingShard() {
        return  messagingShard;
    }

    private synchronized void removeRequest(ProviderServices request) {
        unplacedRequests.put(request,true);
        requestsCount--;
    }

    private int countLeftRequests(){
        int leftRequests = 0;
        for(Boolean value : unplacedRequests.values()) {
            if(!value) {
                leftRequests++;
            }
        }
        return  leftRequests;
    }

    private void setSolvedRequests(ProviderServices requests, Boolean status) {
        synchronized (solvedRequests) {
            solvedRequests.put(requests,status);
        }

    }

    private int getSolvedRequestsCount() {
        int solvedRequestsCount = 0;
        synchronized (solvedRequests){
            for(Boolean status: solvedRequests.values()) {
                if(status == true){
                    solvedRequestsCount++;
                }
            }
        }
        return solvedRequestsCount;
    }

    private void finalAnounce() {
        System.out.println(getName() + " a terminat si are: ");
        for(ProviderServices service: solvedRequests.keySet()){
            System.out.println(service.toString() + " " + solvedRequests.get(service));
        }
        System.out.println();
    }

    private  void printInitialState(){
        System.out.println(getName() + " start si are: ");
        for(ProviderServices service: unplacedRequests.keySet()){
            System.out.println(service.toString() + " " + unplacedRequests.get(service));
        }
        System.out.println();
    }

    private void notifySupervisorRequestsComplted(){
        getMessagingShard().sendMessage(getName(), supervisorName, "DONE");
    }

    private int getRequestCount(){
        return initialRequestsCount;
    }

}

