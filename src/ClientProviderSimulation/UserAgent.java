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


public class UserAgent implements Agent {

    private ArrayList<ProviderServices> requests = new ArrayList<ProviderServices>();

    private String name;
    private MessagingPylonProxy pylon;
    private AbstractMessagingShard messagingShard;
    private UserRequestShard userRequestShard;


    private ShardContainer masterProxy = new ShardContainer() {

        @Override
        public String getEntityName() {
            return name;
        }

        @Override
        public void postAgentEvent(AgentEvent event) {

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
        requests.add(request);
    }
}
