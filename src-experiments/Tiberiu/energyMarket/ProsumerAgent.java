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
 * Coordinates localized microgrid auction scenarios deterministically.
 */
public class ProsumerAgent extends BaseAgent {

    protected MessagingShard msgShard;
    private Timer timer;

    /** Generator determinist controlat prin seed. */
    private Random random;

    /** Numele MarketAgent-ului zonal dedus la pornire. */
    private String targetMarketAgent;

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

        // =====================================================================
        // ETAPA DE CONFIGURARE TOPOLOGICĂ ȘI DETERMINISTĂ
        // =====================================================================
        String agentName = this.getName();

        // 1. Initializam generatorul folosind hash-ul numelui ca seed unic si fix
        this.random = new Random(agentName.hashCode());

        // 2. Deducem zona din nume (ex: din "Prosumer_A12" extragem "A")
        String zone = "A"; // fallback nominal
        if (agentName.contains("_")) {
            zone = agentName.split("_")[1].substring(0, 1);
        }
        this.targetMarketAgent = "MarketAgent_" + zone;

        li("Prosumer " + agentName + " joined microgrid zone: " + zone +
                ". Linked market broker target: " + targetMarketAgent);

        // Start periodic interaction sequence every 3 seconds
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                evaluateEnergyNeedsAndBid();
            }
        }, 1000, 3000);

        evaluateEnergyNeedsAndBid();

        return true;
    }

    /**
     * Orchestreaza executia strategiei si trimite oferta catre piata zonala.
     */
    protected void evaluateEnergyNeedsAndBid() {
        // Apelam componenta decizionala decuplata
        String payload = computeEnergyStrategy();

        AgentWave wave = new AgentWave(payload, targetMarketAgent);
        li("Emitting localized offer to [" + targetMarketAgent + "]: " + payload);
        msgShard.sendMessage(wave);
    }

    /**
     * Componenta decizionala (BDI). In prezent genereaza decizii
     * pseudorandomizate perfect deterministe pe baza seed-ului stabil.
     * * PLACEHOLDER PENTRU CERCETARE: Aici poti inlocui intregul corp al metodei
     * cu un apel catre un model LLM (Ollama/Qwen) sau retea DRL,
     * schimband complet comportamentul fara a altera infrastructura platformei.
     */
    private String computeEnergyStrategy() {
        // Cantitate determinista intre 1.0 si 5.0 kWh (un zecimal)
        double amount = Math.round((1.0 + (4.0 * random.nextDouble())) * 10.0) / 10.0;

        // Pret determinist intre 0.50 si 1.50 RON/kWh (doua zecimale)
        double price = Math.round((0.5 + random.nextDouble()) * 100.0) / 100.0;

        // Decizie asimetrica stabila de tip BID/ASK
        String type = random.nextBoolean() ? "BID" : "ASK";

        return type + "|" + amount + "|" + price;
    }

    /**
     * Processes incoming events, including market transaction confirmations.
     */
    protected boolean processEvent(AgentEvent e) {
        // Adaugă asta în processEvent-ul din ProsumerAgent.java pentru a unifica comportamentul:
        if (AgentEventType.AGENT_WAVE.equals(e.getType())) {
            AgentWave wave = (AgentWave) e;
            if ("NEXT_STEP_TICK".equals(wave.getContent())) {
                evaluateEnergyNeedsAndBid(); // Execută logica grea deterministă
            }
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