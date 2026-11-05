package Tiberiu.energyMarket;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;

/**
 * Agent hibrid corectat: nu introduce overhead artificial.
 * Ruleaza pasiv pana la pasul 15, apoi preia comportamentul exact al ProsumerAgent.
 */
public class DynamicBridgeAgent extends ProsumerAgent {

    private boolean isCurrentlyLive = false;
    private int stepCounter = 0;

    @Override
    protected boolean processEvent(AgentEvent e) {
        if (net.xqhs.flash.core.agent.AgentEvent.AgentEventType.AGENT_WAVE.equals(e.getType())) {
            AgentWave wave = (AgentWave) e;

            if ("NEXT_STEP_TICK".equals(wave.getContent())) {
                stepCounter++;

                // La pasul 15, promovam agentul in modul LIVE nominal
                if (!isCurrentlyLive && stepCounter >= 15) {
                    isCurrentlyLive = true;
                    li("[HOT-SWAP] Agent " + getName() + " promoted to LIVE at tick " + stepCounter);
                }

                if (!isCurrentlyLive) {
                    // 1. MODUL MOCKED: Replay pasiv echilibrat (fara calcul, direct pe I/O)
                    String zone = getName().contains("_") ? getName().split("_")[1].substring(0, 1) : "A";
                    String targetMarket = "MarketAgent_" + zone;

                    // Alternam rapid tipul pentru a asigura rularea tranzactiilor in siguranta cat e mocked
                    String type = (stepCounter % 2 == 0) ? "BID" : "ASK";
                    double price = "BID".equals(type) ? 1.20 : 1.10;

                    try {
                        java.lang.reflect.Field field = ProsumerAgent.class.getDeclaredField("msgShard");
                        field.setAccessible(true);
                        net.xqhs.flash.core.support.MessagingShard privateShard =
                                (net.xqhs.flash.core.support.MessagingShard) field.get(this);

                        privateShard.sendMessage(new AgentWave(type + "|2.5|" + price, targetMarket));
                    } catch (Exception ex) {
                        super.evaluateEnergyNeedsAndBid();
                    }
                } else {
                    // 2. MODUL LIVE NOMINAL: Executa fix metoda de business din clasa mama,
                    // fara nicio bucla computationala parazita!
                    super.evaluateEnergyNeedsAndBid();
                }
            }
        }
        return true;
    }
}