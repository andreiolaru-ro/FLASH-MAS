package Tiberiu.energyMarket;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;

public class MarketAgent extends BaseAgent {

    private class Order {
        String agentId; String type; double quantity; double price;
        Order(String id, String t, double q, double p) {
            this.agentId = id; this.type = t; this.quantity = q; this.price = p;
        }
    }

    private MessagingShard msgShard;
    private final List<Order> bids = new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();
    private Timer simulationClock;
    private int currentTickCount = 0;

    private static final AtomicInteger globalTradeCounter = new AtomicInteger(0);
    private static final int MAX_TRADES_LIMIT = 100000;
    private static boolean isShutdownInitiated = false;

    @Override
    public boolean start() {
        if(!super.start()) return false;

        msgShard = (MessagingShard) AgentShardCore.instantiateRecommendedShard(
                StandardAgentShard.MESSAGING, (PylonProxy) getContext(), null,
                new BaseAgentProxy() {
                    @Override
                    public boolean postAgentEvent(AgentEvent event) { return processEvent(event); }
                });

        msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
        li("======= " + this.getName() + " SECTOR OPEN & WAITING FOR INBOUND TRADES =======");

        // Eliminat complet bucla for care dadea eroare! Piața pornește reactiv la primul mesaj sosit.
        return true;
    }

    private void triggerSimulationTick() {
        if (isShutdownInitiated) return;
        currentTickCount++;
        String zone = this.getName().contains("_") ? this.getName().split("_")[1] : "A";

        // REIMPLEMENTAT: Log de control pentru avansarea pasului de simulare
        if (currentTickCount % 20 == 0) { // Logam din 20 in 20 de pasi pentru a nu satura consola
            li(String.format("[CLOCK TICK #%d] Sector %s querying local microgrid nodes...", currentTickCount, zone));
        }

        for (int i = 1; i <= 33; i++) {
            msgShard.sendMessage(new AgentWave("NEXT_STEP_TICK", "Prosumer_" + zone + i));
        }
    }

    protected boolean processEvent(AgentEvent e) {
        if (isShutdownInitiated || !AgentEventType.AGENT_WAVE.equals(e.getType())) return false;

        AgentWave wave = (AgentWave) e;
        String senderId = wave.getCompleteSource();
        String content = wave.getContent();

        String[] parts = content.split("\\|");
        if (parts.length == 3) {
            synchronized (MarketAgent.class) {
                if ("BID".equals(parts[0])) {
                    bids.add(new Order(senderId, parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                } else {
                    asks.add(new Order(senderId, parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                }
                executeMatchingEngine();

                // REPLICARE INSTANT: Imediat ce am procesat ordinul unui agent,
                // îi deblocăm următorul pas fără nicio secundă de așteptare fizică!
                if (!isShutdownInitiated) {
                    msgShard.sendMessage(new AgentWave("NEXT_STEP_TICK", senderId));
                }
            }
        }
        return true;
    }

    private void executeMatchingEngine() {
        while (!bids.isEmpty() && !asks.isEmpty() && !isShutdownInitiated) {
            bids.sort((b1, b2) -> Double.compare(b2.price, b1.price));
            asks.sort((a1, a2) -> Double.compare(a1.price, a2.price));

            Order topBid = bids.get(0);
            Order topAsk = asks.get(0);

            if (topBid.price >= topAsk.price) {
                double clearingPrice = Math.round(((topBid.price + topAsk.price) / 2.0) * 100.0) / 100.0;
                double tradedQuantity = Math.min(topBid.quantity, topAsk.quantity);

                int count = globalTradeCounter.incrementAndGet();

                // REIMPLEMENTAT: Monitorizarea explicita a tranzactiilor in executie
                li(String.format("[MATCH SUCCESS #%d] %s Sector | %s bought %.1f kWh from %s @ %.2f RON",
                        count, this.getName().split("_")[1], topBid.agentId, tradedQuantity, topAsk.agentId, clearingPrice));

                bids.remove(0);
                asks.remove(0);

                if (count >= MAX_TRADES_LIMIT) {
                    isShutdownInitiated = true;
                    if (simulationClock != null) simulationClock.cancel();
                    li("======= MAX WORKLOAD REACHED: 10000 TRADES ATTAINED =======");
                    System.exit(0);
                }
            } else {
                break;
            }
        }
    }
}