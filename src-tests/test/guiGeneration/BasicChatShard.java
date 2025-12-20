package test.guiGeneration;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.gui.GuiShard;
import net.xqhs.flash.remoteOperation.CentralMonitoringAndControlEntity;

public class BasicChatShard extends AgentShardGeneral {
    /** The serial version ID */
    private static final long serialVersionUID = -4289297018444620944L;
    
    private static final String CONFIG_OTHER_AGENT = "otherAgent";

    private String otherAgent;

    /**
     * No-argument constructor.
     */
    public BasicChatShard() {
        super(AgentShardDesignation.autoDesignation("BasicChat"));
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        otherAgent = configuration.getAValue(CONFIG_OTHER_AGENT);
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        GuiShard guiShard = (GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation());
        if (event.getType() == AgentEventType.AGENT_WAVE) {
            AgentWave wave = (AgentWave) event;
            String[] source = wave.getSourceElements();
            li("Received AgentWave from []", wave.getCompleteSource());

            if (DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME.equals(source[0])) {
                handleGUIWave(wave);
            } else if (otherAgent.equals(source[0])) {
                guiShard.sendOutput(new AgentWave(wave.getContent(), "inbound"));
            }
        }
    }

    private void handleGUIWave(AgentWave wave) {
        String[] destination = wave.getDestinationElements();
        if (destination.length < 2) return;

        String port = destination[0];
        String role = destination[1];

        if ("outbound".equals(port) && "true".equals(wave.get("send"))) {
            sendMessage(wave.getContent(), "outbound", otherAgent, "inbound");
        }
    }
}
