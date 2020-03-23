package ClientProviderSimulation;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;

import java.util.ArrayList;
import java.util.HashMap;


public class UserAgent implements Agent {

    private HashMap<ProviderServices, Boolean> unplacedRequests = new HashMap<ProviderServices, Boolean>();
    private HashMap<ProviderServices, Boolean> placedRequests = new HashMap<ProviderServices, Boolean>();
    private int requestsCount = 0;

    private String name;
    private MessagingPylonProxy pylon;
    private AbstractMessagingShard messagingShard;
    private UserRequestShard userRequestShard;
    private static String EMPTY = "";
    private String answaer = EMPTY;


    private ShardContainer masterProxy = new ShardContainer() {

        @Override
        public String getEntityName() {
            return name;
        }

        @Override
        public void postAgentEvent(AgentEvent event) {

            if(event.containsKey(AbstractMessagingShard.CONTENT_PARAMETER)) {
                //printMessage(event);
                /* Verifies if the message represents the acceptance or denial of a request placement */
                if(event.get(AbstractMessagingShard.CONTENT_PARAMETER).equals("YES") ||
                        event.get(AbstractMessagingShard.CONTENT_PARAMETER).equals("NO")) {
                    setAnswaer(event.get(AbstractMessagingShard.CONTENT_PARAMETER));
                } else {
                    /* If the message is not about an accepted or denied request, then it's
                    about the result of a request  */
                    //placedRequests.put(request, true);
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
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void run() {

        synchronized (unplacedRequests){
            while(countLeftRequests() > 0 ) {
                for( ProviderServices request : unplacedRequests.keySet() ) {
                    if(!unplacedRequests.get(request)) {
                        makeRequest(request);
                    }

                }
               // System.out.println(getName() + " has this nr of req left " + requestsCount);
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

        for(int i = 0; i< ClientProviderSimulation.PROVIDER_COUNT; i++) {

            getMessagingShard().sendMessage(getName(), "Provider " + i, request.toString());
            waitForAnswear();
            if(getAnswaer().equals("YES")) {
                removeRequest(request);
                //placedRequests.put(request, false);

                break;
            }
            /* Search for another provider. So the previous answear is no longer needed */
            if(getAnswaer().equals("NO")) {
                setAnswaer(EMPTY);
            }
        }
    }

    private void setAnswaer(String content) {
        answaer = content;
    }

    private String getAnswaer(){
        return  answaer;
    }

    private void waitForAnswear() {
        while(getAnswaer().equals(EMPTY)){
            ;
        }
    }

    private void printMessage(AgentEvent event) {
        System.out.println("["+getName()+"] " + event.get(AbstractMessagingShard.CONTENT_PARAMETER) +
                " de la " + event.get(AbstractMessagingShard.SOURCE_PARAMETER )+ " la " +
                event.get(AbstractMessagingShard.DESTINATION_PARAMETER));
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

}

