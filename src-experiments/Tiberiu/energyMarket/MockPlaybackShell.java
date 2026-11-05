package Tiberiu.energyMarket;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;

public class MockPlaybackShell extends BaseAgent {
    private MessagingShard msgShard;
    private String targetMarketAgent;
    private boolean alternateType = false;
    private int processedTicks = 0;

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

        String name = this.getName();
        String zone = "A";
        if (name.startsWith("Prosumer_")) {
            zone = String.valueOf(name.charAt(9));
        } else if (name.contains("_")) {
            zone = name.split("_")[1].substring(0, 1);
        }
        this.targetMarketAgent = "MarketAgent_" + zone;

        // SELF-BOOTSTRAP: Trimitem primul ordin istoric pentru a deschide canalul cu piata zonala
        alternateType = !alternateType;
        String type = alternateType ? "BID" : "ASK";
        double price = "BID".equals(type) ? 1.20 : 1.10;
        msgShard.sendMessage(new AgentWave(type + "|2.5|" + price, targetMarketAgent));

        return true;
    }

    protected boolean processEvent(AgentEvent e) {
        if(!AgentEventType.AGENT_WAVE.equals(e.getType())) return false;

        if ("NEXT_STEP_TICK".equals(((AgentWave) e).getContent())) {
            processedTicks++;
            alternateType = !alternateType;
            String type = alternateType ? "BID" : "ASK";
            double price = "BID".equals(type) ? 1.20 : 1.10;
            String payload = type + "|2.5|" + price;

            // REIMPLEMENTAT: Afisaj rar de control (o data la 50 de tici) ca sa vezi ca agentul traieste
            if (processedTicks % 50 == 0) {
                li(String.format("[MOCKED REPLAY] Agent %s reacted to tick %d -> Streaming historical event: %s",
                        this.getName(), processedTicks, payload));
            }

            msgShard.sendMessage(new AgentWave(payload, targetMarketAgent));
        }
        return true;
    }
}