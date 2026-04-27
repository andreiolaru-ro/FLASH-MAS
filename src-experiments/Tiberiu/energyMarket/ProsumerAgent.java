package Tiberiu.energyMarket;

import java.util.Random;
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
 * Represents a smart home (Prosumer) in the Peer-to-Peer energy market.
 * This agent periodically generates BIDs (buy requests) or ASKs (sell requests)
 * based on its internal energy balance and sends them to the MarketAgent.
 */
public class ProsumerAgent extends BaseAgent {

    /** The shard responsible for sending and receiving messages. */
    private MessagingShard msgShard;

    /** Random generator for simulating energy production and consumption. */
    private Random random = new Random();

    /** Timer for periodic market interactions. */
    private Timer timer;

    @Override
    public boolean start() {
        if(!super.start()) {
            return false;
        }

        // Initialize the messaging shard to enable communication via AgentWave
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
        li("Prosumer " + this.getName() + " has joined the microgrid.");

        // Start the periodic behavior: evaluate energy needs every 3 seconds
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                evaluateEnergyNeedsAndBid();
            }
        }, 1000, 3000);

        return true;
    }

    /**
     * Simulates the internal BDI (Belief-Desire-Intention) logic.
     * Decides whether to act as a buyer or seller for this time step.
     */
    protected void evaluateEnergyNeedsAndBid() {
        // Simulate energy amount between 1.0 and 5.0 kWh (rounded to 1 decimal)
        double amount = Math.round((1.0 + (4.0 * random.nextDouble())) * 10.0) / 10.0;

        // Simulate acceptable price between 0.50 and 1.50 RON/kWh (rounded to 2 decimals)
        double price = Math.round((0.5 + random.nextDouble()) * 100.0) / 100.0;

        // 50% probability to be a seller (ASK), 50% to be a buyer (BID)
        String type = random.nextBoolean() ? "BID" : "ASK";

        // Format payload as "TYPE|AMOUNT|PRICE" for easy parsing by the market
        String payload = type + "|" + amount + "|" + price;

        AgentWave wave = new AgentWave(payload, "MarketAgent");
        li("Emitting offer to market: [" + type + "] " + amount + " kWh @ " + price + " RON");
        msgShard.sendMessage(wave);
    }

    /**
     * Processes incoming events, specifically market transaction confirmations.
     * * @param e The received event
     * @return true if successfully processed
     */
    protected boolean processEvent(AgentEvent e) {
        if(AgentEventType.AGENT_WAVE.equals(e.getType())) {
            String notification = (String) ((AgentWave) e).getContent();
            li(">>> Notification received: " + notification);
        }
        return true;
    }

    @Override
    public boolean stop() {
        if(timer != null) {
            timer.cancel();
        }
        li("Prosumer " + this.getName() + " shutting down.");
        return super.stop();
    }
}