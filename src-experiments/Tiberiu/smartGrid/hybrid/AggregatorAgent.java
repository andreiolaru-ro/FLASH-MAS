package Tiberiu.smartGrid.hybrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Standard FLASH-MAS local microgrid market clearing agent.
 * Aggregates prosumer bids and asks within its assigned neighborhood zone, broadcasts
 * local clearing prices, and propagates excess imbalance requests to neighboring zone
 * aggregators when local thresholds are exceeded.
 */
public class AggregatorAgent extends BaseAgent {

    private PylonProxy					pylonContext;
    private MessagingShard				msgShard;
    private final List<String>			localProsumers;
    private final List<String>			neighborAggregators;

    private final Map<String, Double>	demandBySender				= new HashMap<>();
    private final Map<String, Double>	bidPriceBySender			= new HashMap<>();
    private final Map<String, Double>	supplyBySender				= new HashMap<>();
    private double						externalDeficitKwh			= 0.0;
    private double						currentClearingPrice		= 0.25;

    /** Per-tick history of computed clearing prices for fidelity benchmarking. */
    private final List<Double>			clearingPriceHistory		= new ArrayList<>();

    private static final double			BASE_GRID_PRICE				= 0.25;
    private static final double			INTER_ZONE_THRESHOLD_KWH	= 35.0;

    public AggregatorAgent(String name, List<String> localProsumers, List<String> neighborAggregators) {
        MultiTreeMap config = new MultiTreeMap();
        config.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, name);
        configure(config);

        this.localProsumers = new ArrayList<>(localProsumers);
        this.neighborAggregators = new ArrayList<>(neighborAggregators);
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof PylonProxy) {
            this.pylonContext = (PylonProxy) context;
        }
        return super.addGeneralContext(context);
    }

    @Override
    public boolean start() {
        if (!super.start())
            return false;
        clearingPriceHistory.clear();
        if (pylonContext != null) {
            msgShard = (MessagingShard) AgentShardCore.instantiateRecommendedShard(
                    StandardAgentShard.MESSAGING,
                    pylonContext,
                    null,
                    new BaseAgentProxy() {
                        @Override
                        public boolean postAgentEvent(AgentEvent event) {
                            return processEvent(event);
                        }
                    });
            if (msgShard != null) {
                msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
            }
        }
        return true;
    }

    protected boolean processEvent(AgentEvent e) {
        if (!AgentEventType.AGENT_WAVE.equals(e.getType()))
            return false;

        AgentWave wave = (AgentWave) e;
        String content = wave.getContent();
        if (content == null)
            return false;

        if (content.startsWith(HybridMessagingShard.TICK_WAVE_CONTENT)) {
            long tick = Long.parseLong(content.split(":")[1]);
            executeStepLogic(tick);
            return true;
        }

        String sender = wave.getFirstSource();
        if (sender == null || sender.isEmpty()) {
            sender = wave.getCompleteSource();
        }
        if (sender != null && sender.contains("/")) {
            for (String part : sender.split("/")) {
                if (part.startsWith("Prosumer_") || part.startsWith("Aggregator_")) {
                    sender = part;
                    break;
                }
            }
        }
        if (sender == null) {
            sender = "unknown";
        }

        String[] parts = content.split("\\|");
        if ("BID".equals(parts[0]) && parts.length >= 3) {
            double qty = Double.parseDouble(parts[1]);
            double price = Double.parseDouble(parts[2]);
            demandBySender.put(sender, qty);
            bidPriceBySender.put(sender, price);
            supplyBySender.remove(sender);
        } else if ("ASK".equals(parts[0]) && parts.length >= 3) {
            double qty = Double.parseDouble(parts[1]);
            supplyBySender.put(sender, qty);
            demandBySender.remove(sender);
            bidPriceBySender.remove(sender);
        } else if ("INTER_ZONE_REQ".equals(parts[0]) && parts.length >= 2) {
            externalDeficitKwh += Double.parseDouble(parts[1]);
        }
        return true;
    }

    private void executeStepLogic(long tick) {
        double totalDemandKwh = externalDeficitKwh;
        double weightedBidPriceSum = externalDeficitKwh * BASE_GRID_PRICE;
        for (Map.Entry<String, Double> entry : demandBySender.entrySet()) {
            double qty = entry.getValue();
            double price = bidPriceBySender.getOrDefault(entry.getKey(), BASE_GRID_PRICE);
            totalDemandKwh += qty;
            weightedBidPriceSum += qty * price;
        }

        double totalSupplyKwh = 0.0;
        for (double qty : supplyBySender.values()) {
            totalSupplyKwh += qty;
        }

        double netImbalance = totalDemandKwh - totalSupplyKwh;

        double rawAvgBid = totalDemandKwh > 0 ? (weightedBidPriceSum / totalDemandKwh) : BASE_GRID_PRICE;
        // Anchor bid average to wholesale grid tariff to prevent unbounded recursive price spirals
        double anchoredBid = 0.5 * BASE_GRID_PRICE + 0.5 * Math.min(rawAvgBid, 0.60);
        double scarcityFactor = 1.0 + (netImbalance / 120.0);
        currentClearingPrice = Math.max(0.10, Math.min(0.85, anchoredBid * scarcityFactor));

        int idx = (int) tick;
        while (clearingPriceHistory.size() <= idx) {
            clearingPriceHistory.add(currentClearingPrice);
        }
        clearingPriceHistory.set(idx, currentClearingPrice);

        // Broadcast local clearing price to zone prosumers
        String clearMsg = String.format(Locale.US, "CLEAR|%.3f", currentClearingPrice);
        for (String prosumer : localProsumers) {
            if (msgShard != null) {
                msgShard.sendMessage(new AgentWave(clearMsg, prosumer));
            }
        }

        // Propagate excess imbalance to neighboring zones
        if (Math.abs(netImbalance) > INTER_ZONE_THRESHOLD_KWH && !neighborAggregators.isEmpty()) {
            double transferPerNeighbor = (netImbalance - Math.signum(netImbalance) * INTER_ZONE_THRESHOLD_KWH)
                    / neighborAggregators.size();
            String interZoneMsg = String.format(Locale.US, "INTER_ZONE_REQ|%.3f", transferPerNeighbor);
            for (String neighbor : neighborAggregators) {
                if (msgShard != null) {
                    msgShard.sendMessage(new AgentWave(interZoneMsg, neighbor));
                }
            }
        }

        demandBySender.clear();
        bidPriceBySender.clear();
        supplyBySender.clear();
        externalDeficitKwh = 0.0;
    }

    public List<Double> getClearingPriceHistory() {
        return Collections.unmodifiableList(clearingPriceHistory);
    }
}