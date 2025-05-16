package net.xqhs.flash.fedlearn;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

public class FedClientShard extends AgentShardGeneral {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Shard designation,
	 */
	public static final String	DESIGNATION			= "Fed:Client";
	
	/**
	 * The node-local {@link FedDriver} instance.
	 */
	FedDriver					fedDriver;
	
	/**
	 * No-arg constructor.
	 */
	public FedClientShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		if(context instanceof FedDriver) {
			fedDriver = (FedDriver) context;
			li("Fed Driver detected");
			return true;
		}
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		
		switch(event.getType()) {
		case AGENT_START:
			// do start procedures
			break;
		case AGENT_WAVE:
			if(DESIGNATION.equals(event.getValue(AgentWave.DESTINATION_ELEMENT))) {
				AgentWave wave = (AgentWave) event;
				lf("processing wave ", wave);
				// TODO process message
			}
			break;
		
		default:
			break;
		}
	}
}
