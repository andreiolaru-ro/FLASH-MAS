package websocketTest;

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

public class AgentTest implements Agent
{
    private String                  name;
    private AbstractMessagingShard  messagingShard;
    private MessagingPylonProxy     pylon;
    public ShardContainer           proxy	= new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event)
        {
            if(event instanceof AgentWave)
                System.out.println(((AgentWave) event).getContent() + " de la "
                        + ((AgentWave) event).getCompleteSource() + " la "
                        + ((AgentWave) event).getCompleteDestination());
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }

        @Override
        public String getEntityName()
        {
            return getName();
        }

    };

    public AgentTest(String name) {
        this.name = name;
    }

    @Override
    public boolean start() {
        if(name.equals("Two")) {
            messagingShard.sendMessage(messagingShard.getAgentAddress(), "One", "Hello from the other side!");
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
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
        if(messagingShard != null) {
            messagingShard.addGeneralContext(pylon);
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

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Agent> asContext()
    {
        return proxy;
    }

    public boolean addMessagingShard(AbstractMessagingShard shard)
    {
        messagingShard = shard;
        shard.addContext(proxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    protected AbstractMessagingShard getMessagingShard()
    {
        return messagingShard;
    }
}
