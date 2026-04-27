package Tiberiu.energyMarket;

import java.util.ArrayList;
import java.util.List;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;

/**
 * Represents the centralized or decentralized exchange platform.
 * Implements a Continuous Double Auction (CDA) mechanism for matching
 * Peer-to-Peer energy trades.
 */
public class MarketAgent extends BaseAgent {

    /**
     * Internal Data Structure representing a single energy order on the market.
     */
    private class Order {
        String agentId;
        String type;
        double quantity;
        double price;

        Order(String id, String t, double q, double p) {
            this.agentId = id;
            this.type = t;
            this.quantity = q;
            this.price = p;
        }
    }

    private MessagingShard msgShard;

    // Order Books
    private List<Order> bids = new ArrayList<>(); // Buyers willing to pay up to X
    private List<Order> asks = new ArrayList<>(); // Sellers wanting at least Y

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
        li("MarketAgent initialized. The P2P Energy Exchange is now OPEN.");
        return true;
    }

    /**
     * Listens for incoming BIDs and ASKs and attempts to clear the market.
     */
    protected boolean processEvent(AgentEvent e) {
        if(!AgentEventType.AGENT_WAVE.equals(e.getType())) {
            return false;
        }

        AgentWave wave = (AgentWave) e;
        String senderId = wave.getCompleteSource();
        String content = wave.getContent();

        // Parse the incoming payload "TYPE|AMOUNT|PRICE"
        String[] parts = content.split("\\|");
        if (parts.length == 3) {
            try {
                String type = parts[0];
                double amount = Double.parseDouble(parts[1]);
                double price = Double.parseDouble(parts[2]);

                Order newOrder = new Order(senderId, type, amount, price);

                if ("BID".equals(type)) {
                    bids.add(newOrder);
                } else if ("ASK".equals(type)) {
                    asks.add(newOrder);
                }

                // Run the matching engine every time a new order enters the book
                executeMatchingEngine();

            } catch (NumberFormatException ex) {
                li("Error parsing order from " + senderId + ": " + content);
            }
        }
        return true;
    }

    /**
     * Core logic for Continuous Double Auction (CDA).
     * Sorts the order books and checks if the highest bid >= lowest ask.
     */
    private void executeMatchingEngine() {
        // We need at least one buyer and one seller to make a trade
        if (bids.isEmpty() || asks.isEmpty()) {
            return;
        }

        // Sort Bids descending (Highest bidder is first in line)
        bids.sort((b1, b2) -> Double.compare(b2.price, b1.price));

        // Sort Asks ascending (Cheapest seller is first in line)
        asks.sort((a1, a2) -> Double.compare(a1.price, a2.price));

        Order topBid = bids.get(0);
        Order topAsk = asks.get(0);

        // Check for market overlap: is the buyer willing to pay what the seller asks?
        if (topBid.price >= topAsk.price) {
            // Determine the clearing price (usually the average in a simple CDA)
            double clearingPrice = Math.round(((topBid.price + topAsk.price) / 2.0) * 100.0) / 100.0;
            double tradedQuantity = Math.min(topBid.quantity, topAsk.quantity);

            li("$$$ TRADE EXECUTED: " + topBid.agentId + " bought " + tradedQuantity +
                    " kWh from " + topAsk.agentId + " at " + clearingPrice + " RON");

            // =====================================================================
            // INTEGRATION POINT FOR RESEARCH:
            // Here is where RecorderService will capture the causal link
            // between Agent A and Agent B for verification with EasyLog.
            // =====================================================================

            // Notify both parties of the successful trade
            String successMsg = "TRADE_SUCCESS | Cleared at " + clearingPrice + " RON";
            msgShard.sendMessage(new AgentWave(successMsg, topBid.agentId));
            msgShard.sendMessage(new AgentWave(successMsg, topAsk.agentId));

            // For simplicity in this PoC, we remove the matched orders entirely.
            // A full implementation would decrement the tradedQuantity.
            bids.remove(0);
            asks.remove(0);
        }
    }
}