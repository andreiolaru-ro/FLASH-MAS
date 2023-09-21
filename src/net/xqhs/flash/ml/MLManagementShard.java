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
	private static final String	DESIGNATION			= "ML:Management";
	/**
	 * The node-local {@link MLDriver} instance.
	 */
	MLDriver					mlDriver;
	/**
	 * The node-local {@link OntologyDriver} instance.
	 */
	OntologyDriver				ontDriver;
	
	public MLManagementShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		if(context instanceof MLDriver) {
			mlDriver = (MLDriver) context;
			li("ML Driver detected");
			return true;
		}
		if(context instanceof OntologyDriver) {
			ontDriver = (OntologyDriver) context;
			li("Ontology Driver detected");
			return true;
		}
		return false;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		// TODO if event is output from agent ML pipeline, evaluate and decide
	}
}
