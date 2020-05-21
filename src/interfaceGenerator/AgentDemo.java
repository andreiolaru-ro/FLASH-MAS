package interfaceGenerator;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Timer;
import java.util.TimerTask;

public class AgentDemo implements Agent {
    private final static long delay = 0;
    private final static long period = 100;
    private Timer timer;
    private String agentName;

    public AgentDemo(MultiTreeMap configuration) {
        agentName = configuration.get("name");
    }

    @Override
    public boolean start() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("ia la pl " + getName());
            }
        }, delay, period);
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public String getName() {
        return agentName;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        return false;
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
