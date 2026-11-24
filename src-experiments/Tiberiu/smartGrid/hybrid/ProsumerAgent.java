package Tiberiu.smartGrid.hybrid;

import java.util.Locale;

import net.xqhs.flash.core.DeploymentConfiguration;
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
 * Standard FLASH-MAS household prosumer agent equipped with rooftop PV generation
 * and residential battery storage.
 * <p>
 * Remains completely decoupled from hybrid replay mechanics: receives tick triggers
 * and market clearing prices exclusively as standard {@link AgentWave} events through
 * the pylon-recommended {@link MessagingShard}.
 */
public class ProsumerAgent extends BaseAgent {

    private MessagingShard				msgShard;
    private final int					customerId;
    private final String				aggregatorName;
    private final AusgridDatasetLoader	dataset;
    private final boolean				perturbed;

    private double						batterySocKwh		= 2.5;
    private static final double			BATTERY_MAX_KWH		= 10.0;
    private double						lastClearingPrice	= 0.25;

    public ProsumerAgent(String name, int customerId, String aggregatorName,
                         AusgridDatasetLoader dataset, boolean perturbed) {
        MultiTreeMap config = new MultiTreeMap();
        config.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, name);
        configure(config);

        this.customerId = customerId;
        this.aggregatorName = aggregatorName;
        this.dataset = dataset;
        this.perturbed = perturbed;
    }

    private PylonProxy pylonContext;

    @Override
    public boolean addGeneralContext(EntityProxy<? extends net.xqhs.flash.core.Entity<?>> context) {
        if (context instanceof PylonProxy) {
            this.pylonContext = (PylonProxy) context;
        }
        return super.addGeneralContext(context);
    }

    @Override
    public boolean start() {
        if (!super.start())
            return false;
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

    /**
     * Processes incoming waves, distinguishing external tick triggers from market clearing updates.
     */
    protected boolean processEvent(AgentEvent e) {
        if (!AgentEventType.AGENT_WAVE.equals(e.getType()))
            return false;

        String content = ((AgentWave) e).getContent();
        if (content == null)
            return false;

        if (content.startsWith(HybridMessagingShard.TICK_WAVE_CONTENT)) {
            long tick = Long.parseLong(content.split(":")[1]);
            executeStepLogic(tick);
            return true;
        }

        if (content.startsWith("CLEAR|")) {
            String[] parts = content.split("\\|");
            if (parts.length >= 2) {
                lastClearingPrice = Double.parseDouble(parts[1]);
            }
        }
        return true;
    }

    /**
     * Evaluates net household energy balance for the given tick, dispatches residential battery storage,
     * and submits a BID or ASK order to the local zone aggregator.
     */
    private void executeStepLogic(long tick) {
        double netEnergy = dataset.getNetEnergy(customerId, tick);

        // Battery dispatch policy: perturbed agents perform aggressive peak charging
        double batteryDelta = 0.0;
        if (perturbed && (tick % 48 >= 34 && tick % 48 <= 40)) {
            batteryDelta = -3.5;
        } else if (netEnergy > 0 && batterySocKwh < BATTERY_MAX_KWH) {
            double charge = Math.min(netEnergy * 0.5, BATTERY_MAX_KWH - batterySocKwh);
            batterySocKwh += charge;
            batteryDelta = -charge;
        } else if (netEnergy < 0 && batterySocKwh > 1.0) {
            double discharge = Math.min(Math.abs(netEnergy) * 0.4, batterySocKwh - 1.0);
            batterySocKwh -= discharge;
            batteryDelta = discharge;
        }

        double marketTradeKwh = netEnergy + batteryDelta;
        if (Math.abs(marketTradeKwh) < 0.05)
            return;

        String orderType = marketTradeKwh > 0 ? "ASK" : "BID";
        double quantity = Math.abs(marketTradeKwh);
        double price = orderType.equals("BID")
                ? lastClearingPrice * (perturbed ? 1.45 : 1.02)
                : lastClearingPrice * 0.95;

        String payload = String.format(Locale.US, "%s|%.3f|%.3f", orderType, quantity, price);
        AgentWave wave = new AgentWave(payload, aggregatorName);
        if (msgShard != null) {
            msgShard.sendMessage(wave);
        }
    }
}