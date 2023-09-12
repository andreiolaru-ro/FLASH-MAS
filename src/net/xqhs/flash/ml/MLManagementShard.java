/**
 * 
 */
package net.xqhs.flash.ml;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

public class MLManagementShard extends AgentShardGeneral {
	

	/**
	 * The serial UID
	 */
	private static final long serialVersionUID = 3728868349364945506L;

	
	public MLManagementShard() {
		super(AgentShardDesignation.customShard("ML:Management"));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		// TODO
		// retain reference to MLDriver and OntologyDriver
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		// TODO if event is output from agent ML pipeline, evaluate and decide
	}
}
