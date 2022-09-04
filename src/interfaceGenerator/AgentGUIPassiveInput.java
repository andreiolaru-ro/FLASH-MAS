package interfaceGenerator;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class AgentGUIPassiveInput extends AgentGUI {
    public AgentGUIPassiveInput(MultiTreeMap configuration) {
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
                Optional<String> port = Utils.randomPort();
                if (port.isPresent()) {
                    AgentWave passiveInput = null;
                    try {
                        passiveInput = ioShard.getInput(port.get());
                        if (passiveInput != null) {
                            Set<String> contentKeys = passiveInput.getKeys();
                            contentKeys.remove("EVENT_TYPE");
                            System.out.println(contentKeys);
                            for (String key : contentKeys) {
                                System.out.println(key + ": " + passiveInput.getValues(key));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, delay, period);
        return true;
    }
}
