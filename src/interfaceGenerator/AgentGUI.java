package interfaceGenerator;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.text.ParseException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class AgentGUI implements Agent {
    private final static long delay = 0;
    private final static long period = 10000;
    private GUIShard guiShard;
    private Timer timer;

    // TODO: trigger page generation

    public AgentGUI(MultiTreeMap configuration) {
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
    public boolean start() {
        // launch the page
        guiShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // TODO: add action for output and passive input
                /*
                var port = Element.randomPort();
                if (port.isPresent()) {
                    AgentWave passiveInput = null;
                    try {
                        passiveInput = guiShard.getInput(port.get());
                        if (passiveInput != null) {
                            var contentKeys = passiveInput.getKeys();
                            contentKeys.remove("EVENT_TYPE");
                            System.out.println(contentKeys);
                            for (var key : contentKeys) {
                                System.out.println(key + ": " + passiveInput.getValues(key));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } */

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
