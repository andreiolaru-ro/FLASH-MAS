package interfaceGenerator;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Timer;
import java.util.TimerTask;

public class AgentQuickMessage extends AgentGUI {
    public AgentQuickMessage(MultiTreeMap configuration) {
        super(configuration);
    }

    @Override
    public boolean start() {
        // launch the page
        guiShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            }
        }, delay, period);
        return true;
    }
}
