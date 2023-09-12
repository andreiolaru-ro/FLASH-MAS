package aifolk.scenario;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;


public class ScenarioDriver extends EntityCore<Node> implements EntityProxy<ScenarioDriver> {

	@Override
	public boolean start() {
		if(!super.start())
			return false;
		li("Scenario driver up");
		// TODO
		// set up timer,
		// send events via registered scenario shards
		return true;
	}
	
	public boolean registerAgent(ScenarioShard scenarioShard) {
		// TODO
		// add to agent list
		return true;
	}
	
	public void receiveAgentOutput(Object output) {
		// TODO evaluate
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		// TODO
		// send scenario termination events via scenario shards
		li("Scenario driver stopped");
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<ScenarioDriver> asContext() {
		return this;
	}
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
}
