package net.xqhs.flash.gui;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.IOShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.gui.structure.Element;

public class GuiShard extends IOShard {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -2769555908800271606L;
	
	protected Element interfaceStructure;
	
	public GuiShard() {
		super(StandardAgentShard.GUI.toAgentShardDesignation());
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		interfaceStructure = GUILoad.load(configuration, getLogger());
		if(interfaceStructure == null) {
			le("Interface load failed");
			return false;
		}
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		// if(event.getType() == AgentEventType.AGENT_START)
		// ((MonitoringShard) getAgentShard(StandardAgentShard.MONITORING.toAgentShardDesignation()))
		// .sendGuiUpdate(new Yaml().dump(interfaceStructure));
	}
	
	@Override
	public AgentWave getInput(String portName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void sendOutput(AgentWave agentWave) {
		// TODO Auto-generated method stub
		
	}
}
