/**
 * 
 */
package aifolk.scenario;

import aifolk_core.OntologyDriver;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.ml.MLPipelineShard;

@SuppressWarnings("javadoc")
public class ScenarioShard extends AgentShardGeneral {
	/**
	 * The serial UID
	 */
	private static final long	serialVersionUID	= -4891703380660803677L;
	public static final String	DESIGNATION			= "Scenario";
	/**
	 * The node-local {@link OntologyDriver} instance.
	 */
	ScenarioDriver				scenarioDriver;
	
	public ScenarioShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		if(context instanceof ScenarioDriver) {
			scenarioDriver = (ScenarioDriver) context;
			li("Scenario Driver detected");
			scenarioDriver.registerAgent(getAgent().getEntityName(), this);
			return true;
		}
		return true;
	}
	
	public void initiateAgentEvent(Object input, long eventID) {
		// TODO event IDs are specific to the agent and should be mananged internally
		AgentWave event = new AgentWave(null, MLPipelineShard.DESIGNATION);
		event.addSourceElementFirst(DESIGNATION);
		event.add("ID", Long.valueOf(eventID).toString()).addObject("input", input);
		if(!getAgent().postAgentEvent(event))
			le("Post event with ID [] failed.", Long.valueOf(eventID));
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		if(AgentEventType.AGENT_WAVE.equals(event.getType())
				&& MLPipelineShard.DESIGNATION.equals(event.getValue(AgentWave.SOURCE_ELEMENT))) {
			String inputID = event.get("ID");
			scenarioDriver.receiveAgentOutput(event.getObject(AgentWave.CONTENT), Long.parseLong(inputID));
		}
	}
}