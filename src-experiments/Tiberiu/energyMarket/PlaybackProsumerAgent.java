package Tiberiu.energyMarket;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;

/**
 * Represents a "Mocked" agent in the Hybrid Simulation.
 * Instead of running real BDI logic, this agent replays historical data
 * (simulating reading from a JSON log file) to generate market background noise
 * with zero computational overhead for decision-making.
 */
public class PlaybackProsumerAgent extends BaseAgent {

    private MessagingShard msgShard;
    private Timer timer;

    // Simulated historical log data (Type|Amount|Price)
    private final String[] historicalLogs = {
            "BID|2.5|0.80",
            "ASK|1.5|0.75",
            "BID|3.0|0.85",
            "ASK|4.0|0.70",
            "BID|1.0|0.90"
    };

    // Keeps track of the current line in the "log file"
    private int playbackCursor = 0;

    @Override
    public boolean start() {
        if(!super.start()) {
            return false;
        }

        msgShard = (MessagingShard) AgentShardCore.instantiateRecommendedShard(
                StandardAgentShard.MESSAGING,
                (PylonProxy) getContext(),
                null,
                new BaseAgentProxy() {
                    @Override
                    public boolean postAgentEvent(AgentEvent event) {
                        return processEvent(event);
                    }
                });

        msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
        li("[MOCKED] PlaybackProsumer " + this.getName() + " started. Replaying logs...");

        timer = new Timer();
        // Replay one log entry every 2.5 seconds
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                replayNextLogEntry();
            }
        }, 2000, 2500);

        return true;
    }

    /**
     * Reads the next entry from the simulated log and injects it into the market.
     */
    private void replayNextLogEntry() {
        if (playbackCursor >= historicalLogs.length) {
            // For continuous demo purposes, loop back to the start of the log
            playbackCursor = 0;
        }

        String payload = historicalLogs[playbackCursor];
        AgentWave wave = new AgentWave(payload, "MarketAgent");

        // Use a distinct log message to easily spot mocked agents in the console
        li("~~~ [PLAYBACK] Injecting historical data: " + payload);
        msgShard.sendMessage(wave);

        playbackCursor++;
    }

    protected boolean processEvent(AgentEvent e) {
        if(AgentEventType.AGENT_WAVE.equals(e.getType())) {
            // Mocked agents ignore market notifications because they are just playing back past events
            // We keep this method empty to save CPU cycles.
        }
        return true;
    }

    @Override
    public boolean stop() {
        if(timer != null) {
            timer.cancel();
        }
        return super.stop();
    }
}