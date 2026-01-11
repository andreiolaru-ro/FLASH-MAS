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


			if (DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME.equals(source[0]) || "gui".equals(source[0])) {
				handleGUIWave(wave);
			}
			else {
				String senderName = source[0];
				guiShard.sendOutput(new AgentWave(senderName + ": " + wave.getContent(), "inbound"));
			}
        }
    }

	private void handleGUIWave(AgentWave wave) {
		String[] source = wave.getSourceElements();

		boolean portFound = false;
		for (String element : source) {
			if ("outbound".equals(element)) {
				portFound = true;
				break;
			}
		}

		boolean sendPressed = wave.containsKey("send");

		if (portFound && sendPressed) {
			String msgContent = (String) wave.get("content");

			if (msgContent == null) {
				String raw = wave.getContent();
				if (raw != null) {
					msgContent = raw;
				}
			}

			if (msgContent != null && !msgContent.isEmpty()) {
				sendMessage(msgContent, "outbound", otherAgent, "inbound");
			}
		}
	}
}
