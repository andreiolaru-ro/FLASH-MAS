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
	/** Configuration key for the other agent name. */
	private static final String CONFIG_OTHER_AGENT = "otherAgent";
	/** The name of the other agent to chat with. */
    private String otherAgent;
	/** Port name for sending outgoing messages. */
	public static final String PORT_OUT = "outbound";
	/** Port name for receiving incoming messages. */
	public static final String PORT_IN = "inbound";

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


			if (DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME.equals(source[0]) || StandardAgentShard.GUI.toAgentShardDesignation().toString().equals(source[0])) {
				handleGUIWave(wave);
			}
			else {
				String senderName = source[0];
				guiShard.sendOutput(new AgentWave(senderName + ": " + wave.getContent(), PORT_IN) );
			}
		}
	}

	private void handleGUIWave(AgentWave wave) {
		String[] source = wave.getSourceElements();

		boolean portFound = false;
		for (String element : source) {
			if (PORT_OUT.equals(element)) {
				portFound = true;
				break;
			}
		}

		boolean sendPressed = wave.containsKey(GuiShard.ROLE_ACTIVATE);

		if (portFound && sendPressed) {
			String msgContent = wave.getContent();

			if (msgContent == null) {
				String raw = wave.getContent();
				if (raw != null) {
					msgContent = raw;
				}
			}

			if (msgContent != null && !msgContent.isEmpty()) {
				sendMessage(msgContent, PORT_OUT, otherAgent, PORT_IN);
			}
		}
	}
}
