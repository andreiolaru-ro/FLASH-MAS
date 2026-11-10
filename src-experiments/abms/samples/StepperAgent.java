package abms.samples;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import testing.AgentPingPongPlain;

public class StepperAgent extends AgentPingPongPlain implements SteppableEntity, ShardContainer {
	
	protected EnvironmentLinkShard e = new EnvironmentLinkShard();
	
	// FIXME why not working with no-arg constructor
	public StepperAgent(MultiTreeMap configuration) {
		super();
		super.configure(configuration);
		e.addContext(this);
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		System.out.println("Add context " + context.getEntityName());
		e.addGeneralContext(context);
		if(context instanceof Simulation)
			((Simulation) context).registerEntity(CategoryName.AGENT.s(), this, this.getName());
		return super.addGeneralContext(context);
	}
	
	@Override
	public void step() {
		if(otherAgents != null)
			sendPing();
	}
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
	@Override
	public boolean postAgentEvent(AgentEvent event) {
		if(event instanceof AgentWave)
			return super.processEvent((AgentWave) event);
		return false;
	}
	
	@Override
	public AgentShard getAgentShard(AgentShardDesignation designation) {
		return null;
	}
	
}
