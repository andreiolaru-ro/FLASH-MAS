/**
 * 
 */
package aifolk.scenario;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

public class ScenarioShard extends AgentShardGeneral {
	
	/**
	 * The serial UID
	 */
	private static final long serialVersionUID = -4891703380660803677L;
	
	public ScenarioShard() {
		super(AgentShardDesignation.customShard("Scenario"));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		// TODO
		// if context is a ScenarioDriver, call register
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		// TODO if event is output from agent ML pipeline, call ScenarioDriver.receiveAgentOutput
	}
}
