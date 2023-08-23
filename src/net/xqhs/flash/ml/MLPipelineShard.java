/**
 * 
 */
package net.xqhs.flash.ml;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

public class MLPipelineShard extends AgentShardGeneral {
	

	/**
	 * The serial UID
	 */
	private static final long serialVersionUID = 4668752071930508849L;
	
	public MLPipelineShard() {
		super(AgentShardDesignation.customShard("ML:Pipeline"));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		// TODO
		// if context is an MLDriver, retain reference
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		// TODO if event input for the ML pipeline, process input and generate output
	}
}
