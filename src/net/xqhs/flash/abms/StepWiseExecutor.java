package net.xqhs.flash.abms;

import java.util.LinkedList;
import java.util.List;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

public class StepWiseExecutor extends EntityCore<Node> implements Executor, EntityProxy<StepWiseExecutor> {
	
	protected static final String	STEPS_PARAM	= "steps";
	protected List<StepAgent>		agentList	= new LinkedList<>();
	protected List<AgentGroup>		groupList	= new LinkedList<>();
	int								nSteps;
	Thread							executor;
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		nSteps = configuration.containsKey(STEPS_PARAM) ? Integer.parseInt(configuration.getAValue(STEPS_PARAM)) : 100;
		return true;
	}
	
	@Override
	public boolean register(Agent agent) {
		agentList.add((StepAgent) agent);
		return true;
	}
	
	@Override
	public boolean register(AgentGroup group) {
		groupList.add(group);
		return true;
	}
	
	@Override
	public boolean start() {
		super.start();
		li("Starting executor with [] groups and total of [] agents.", groupList.size(), agentList.size());
		// groupList.stream().forEach(g -> g.prepareExecution());
		
		int nStarted = 0;
		for(Agent agent : agentList) {
			if(agent.start())
				nStarted++;
			else
				le("Agent [] could not be started.", agent.getName());
		}
		li("Started [] agents out of [].", Integer.valueOf(nStarted), Integer.valueOf(agentList.size()));
		
		executor = new Thread() {
			@Override
			public void run() {
				for(int step = 0; step < nSteps; step++) {
					li("Step []", step);
					agentList.stream().forEach(a -> a.preStep());
					agentList.stream().forEach(a -> a.step());
					groupList.stream().forEach(g -> g.display());
				}
				
			}
		};
		executor.start();
		return true;
	}
	
	@Override
	public boolean stop() {
		int nStopped = 0;
		for(Agent agent : agentList) {
			if(agent.stop())
				nStopped++;
			else
				le("Agent [] could not be stopped.", agent.getName());
		}
		li("Stopped [] agents out of [].", Integer.valueOf(nStopped), Integer.valueOf(agentList.size()));
		try {
			executor.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		return super.stop();
	}
	
	@Override
	public EntityProxy<StepWiseExecutor> asContext() {
		return this;
	}
}
