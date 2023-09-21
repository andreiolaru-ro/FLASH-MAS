package aifolk.scenario;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;


public class ScenarioDriver extends EntityCore<Node> implements EntityProxy<ScenarioDriver> {

	Map<String, ScenarioShard> agents = new HashMap<>();
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		li("Scenario driver up");
		// TODO
		// set up timer,
		// send events via registered scenario shards
		
		// FIXME mockup
		String agent = "A";

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				agents.get(agent).initiateAgentEvent(null, 1); // 1 is the event ID
				timer.cancel();
			}
		}, 2000);
		
		return true;
	}
	
	public boolean registerAgent(String agentName, ScenarioShard scenarioShard) {
		agents.put(agentName, scenarioShard);
		li("Registered agent [] with shard [].", agentName, scenarioShard);
		return true;
	}
	
	public void receiveAgentOutput(Object output, long ID) {
		li("Obtained output for ID ", ID);
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
