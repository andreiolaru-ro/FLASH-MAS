package net.xqhs.flash.core.shard;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;

public abstract class IOShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -2775487340051334684L;
	
	protected IOShard() {
		super(StandardAgentShard.IO.toAgentShardDesignation());
	}
	
	protected IOShard(AgentShardDesignation designation) {
		super(designation);
	}
	
	public void postActiveInput(String port, Map<String, String> values) {
		AgentWave inputWave = new AgentWave(null, "/");
		for(String role : values.keySet())
			inputWave.add(role, values.get(role));
		postActiveInput(port, inputWave);
	}
	
	public void postActiveInput(String port, AgentWave inputWave) {
		if(inputWave.getCompleteSource().length() == 0)
			inputWave.addSourceElementFirst(port);
		inputWave.addSourceElementFirst(getShardDesignation().toString());
		super.getAgent().postAgentEvent(inputWave);
	}
	
	public abstract AgentWave getInput(String portName);
	
	public abstract void sendOutput(AgentWave agentWave);
	
	public static HashMap<String, String> reducedInterfacesValues = new HashMap<>();
}
