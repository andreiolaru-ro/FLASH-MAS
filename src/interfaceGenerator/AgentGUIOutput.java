package interfaceGenerator;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.text.ParseException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class AgentGUIOutput extends AgentGUI {
    public AgentGUIOutput(MultiTreeMap configuration) {
        super(configuration);
    }

    @Override
    public boolean start() {
        guiShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AgentWave agentWave = new AgentWave();
                // some rubbish values for testing
                agentWave.add("chicken", TestUtils.getAlphaNumericString(10));
                agentWave.add("nuggets", TestUtils.getAlphaNumericString(10));
                agentWave.add("kfc", String.valueOf(new Random().nextInt(30)));
                try {
                    guiShard.sendOutput(agentWave);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }, delay, period);
        return true;
    }
}
