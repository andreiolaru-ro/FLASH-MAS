/**
 * 
 */
package net.xqhs.flash.ml;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

public class MLPipelineShard extends AgentShardGeneral {
	/**
	 * The serial UID
	 */
	private static final long	serialVersionUID	= 4668752071930508849L;
	public static final String	DESIGNATION			= "ML:Pipeline";
	/**
	 * The node-local {@link MLDriver} instance.
	 */
	MLDriver					mlDriver;
	
	public MLPipelineShard() {
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
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		if(event.getType() == AgentEventType.AGENT_WAVE
				&& DESIGNATION.equals(event.getValue(AgentWave.DESTINATION_ELEMENT))) {
			String inputID = event.get("ID");
			lf("input received with ID ", inputID);
			// TODO process input and generate output
			
			// FIXME mockup
			AgentWave output = new AgentWave();
			output.add("ID", Long.valueOf(inputID).toString()).addObject("output", null);
			output.addSourceElements(DESIGNATION);
			if(!getAgent().postAgentEvent(output))
				le("Post output event with ID [] failed.", inputID);
		}
	}
}
