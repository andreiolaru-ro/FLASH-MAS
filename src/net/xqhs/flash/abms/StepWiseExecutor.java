package net.xqhs.flash.abms;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.util.MultiTreeMap;

public class StepWiseExecutor extends EntityCore<Simulation>
		implements SimulationExecutor, EntityProxy<StepWiseExecutor> {

	protected static final String	STEPS_PARAM	= "steps";
	int								nSteps;
	Thread							executor;
	Simulation						simulation;
	
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
	public boolean addContext(EntityProxy<Simulation> context) {
		simulation = (Simulation) context;
		simulation.registerExecutor(this);
		// TODO why this does not work
		return true;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		super.addGeneralContext(context);
		if(!(context instanceof Simulation))
			return true;
		simulation = (Simulation) context;
		simulation.registerExecutor(this);
		return true;
	}
	
	@Override
	public boolean start() {
		super.start();
		li("Starting executor with [] contexts and [] agents.", simulation.getSimulationContexts().size(),
				simulation.getSimulationObjects().size());
		
		// TODO send suspend signal to non-step agents
		
		executor = new Thread() {
			@Override
			public void run() {
				for(long step = 0; step < nSteps; step++)
					runStep(step);
				simulation.executionCompleted();
			}
		};
		executor.start();
		return true;
	}
	
	protected void runStep(long step) {
		li("Step []", Long.valueOf(step));
		for (Entity<?> entity : simulation.getSimulationObjects()) {
			for (SimulationContext context : simulation.getSimulationContexts())
				context.sendEvents(entity);//push pending events to the entity
			if (entity instanceof SteppableEntity)
				((SteppableEntity) entity).step();//then execute the entity's step

			else if (entity instanceof Patch)
				((Patch) entity).step();
		}
		// all entities have been stepped, now update the simulation contexts
		for (SimulationContext context : simulation.getSimulationContexts())
			context.validateAndExecutePendingActions();
		simulation.stepCompleted();
	}
	
	@Override
	public boolean stop() {
		li("Executor stopped.");
		try {
			executor.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		return super.stop();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<StepWiseExecutor> asContext() {
		return this;
	}
}
