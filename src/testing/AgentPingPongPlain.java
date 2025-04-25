package testing;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * A version of AgentPingPong that uses PylonProxy directly.
 */
public class AgentPingPongPlain extends BaseAgent {

    protected static final String OTHER_AGENT_PARAMETER_NAME = "sendTo";
    protected static final String SHARD_ENDPOINT = "ping";
    protected static final long PING_INITIAL_DELAY = 2000;
    protected static final long PING_PERIOD = 2000;
    protected static final String PING_NUMBER_PARAMETER_NAME = "ping-number";
    protected static final int DEFAULT_PING_NUMBER = 5;

    int pingLimit;

    Timer pingTimer = null;
    List<String> otherAgents = null;
    String agentName = null;
    int tick = 0;

    WaveMessagingPylonProxy pylonProxy = null;

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.isSet(OTHER_AGENT_PARAMETER_NAME))
            otherAgents = configuration.getValues(OTHER_AGENT_PARAMETER_NAME);
        if (configuration.isSet(PING_NUMBER_PARAMETER_NAME)) {
            pingLimit = Integer.parseInt(configuration.getFirstValue(PING_NUMBER_PARAMETER_NAME));
        } else {
            pingLimit = DEFAULT_PING_NUMBER;
        }
        return true;
    }

    @Override
    public boolean start() {
        if (!super.start())
            return false;
        pylonProxy = (WaveMessagingPylonProxy) getContext();
        if (pylonProxy == null)
            throw new IllegalStateException("No PylonProxy present");
        pylonProxy.register(pylonProxy.getEntityName(), new WaveReceiver() {
            @Override
            public void receive(AgentWave wave) {
                processEvent(wave);
            }
        });
        if (otherAgents != null) {
            pingTimer = new Timer();
            pingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendPing();
                }
            }, PING_INITIAL_DELAY, PING_PERIOD);
        }
        li("Agent started");
        return true;
    }

    protected boolean processEvent(AgentWave wave) {
        li("received: " + wave.toString());
        String replyContent = wave.getContent() + " reply";
        li("sending reply ", wave.createReply(replyContent));
        return pylonProxy.send((wave).createReply(replyContent));
    }

    @Override
    public boolean stop() {
        if (!super.stop())
            return false;
        pingTimer.cancel();
        li("Agent stopped");
        return true;
    }


    protected void sendPing() {
        if (pingLimit >= 0 && tick >= pingLimit) {
            li("Ping limit reached, stopping agent.");
            stop();
            return;
        }

        tick++;
        for (String otherAgent : otherAgents) {
            AgentWave wave = new AgentWave("ping-no " + tick, otherAgent, "pong").addSourceElementFirst("ping");
            lf("Sending the message [] to ", wave, otherAgent);
            if (!pylonProxy.send(wave))
                le("Message sending failed");
        }
    }
}