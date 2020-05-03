package interfaceGenerator;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Timer;

public abstract class AgentGUI implements Agent {
    protected final static long delay = 0;
    protected final static long period = 10000;
    protected GUIShard guiShard;
    protected Timer timer;

    // TODO: trigger page generation

    protected AgentGUI(MultiTreeMap configuration) {
        guiShard = new GUIShard(configuration);
        guiShard.addContext(new ShardContainer() {
            @Override
            public void postAgentEvent(AgentEvent event) {
                guiShard.signalAgentEvent(event);
            }

            @Override
            public AgentShard getAgentShard(AgentShardDesignation designation) {
                return null;
            }

            @Override
            public String getEntityName() {
                return getName();
            }
        });
    }

    @Override
    public abstract boolean start();

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getName() {
        return "AgentGUI";
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        return guiShard.addGeneralContext(context);
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return null;
    }
}
