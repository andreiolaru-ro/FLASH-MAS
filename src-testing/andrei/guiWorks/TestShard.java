package andrei.guiWorks;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.gui.GuiShard;

@SuppressWarnings("javadoc")
public class TestShard extends AgentShardGeneral {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public TestShard() {
		super(AgentShardDesignation.autoDesignation("Test"));
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		if(event.getType() == AgentEventType.AGENT_START)
			((GuiShard) getAgentShard(AgentShardDesignation.autoDesignation("GUI")))
					.sendOutput(new AgentWave(Integer.valueOf(0).toString(), "port1"));
		new Timer().schedule(new TimerTask() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				int value = Integer.parseInt(((GuiShard) getAgentShard(AgentShardDesignation.autoDesignation("GUI")))
						.getInput("port1").get(AgentWave.CONTENT));
				((GuiShard) getAgentShard(AgentShardDesignation.autoDesignation("GUI")))
						.sendOutput(new AgentWave(Integer.valueOf(value + 1).toString(), "port1"));
			}
		}, 0, 1000);
		if(event.getType() == AgentEventType.AGENT_WAVE)
			li("Agent event from []: ", ((AgentWave) event).getCompleteSource(), event);
	}
}
