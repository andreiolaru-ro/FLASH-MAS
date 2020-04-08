package interfaceGenerator;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.config.Config;
import net.xqhs.util.config.Configurable;

public class GuiShard implements AgentShard {
    @Override
    public AgentShardDesignation getShardDesignation() {
        return null;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {

    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

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
        return null;
    }

    @Override
    public boolean addContext(EntityProxy<Agent> context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Agent> context) {
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
    public <C extends Entity<Agent>> EntityProxy<C> asContext() {
        return null;
    }

    @Override
    public Configurable makeDefaults() {
        return null;
    }

    @Override
    public Config lock() {
        return null;
    }

    @Override
    public Config build() {
        return null;
    }

    @Override
    public void ensureLocked() {

    }

    @Override
    public void locked() throws Config.ConfigLockedException {

    }
}
