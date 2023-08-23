package aifolk.scenario;

import net.xqhs.flash.core.ConfigurableEntity;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;


public class ScenarioDriver extends Unit implements ConfigurableEntity<Node>, EntityProxy<ScenarioDriver> {

	@Override
	public boolean configure(MultiTreeMap configuration) {
		setUnitName(configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME));
		return true;
	}
	
	@Override
	public boolean start() {
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
		// TODO
		// send scenario termination events via scenario shards
		return true;
	}
	
	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
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
