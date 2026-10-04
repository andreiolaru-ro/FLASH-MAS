package net.xqhs.flash.abms;

import abms.smartMeeting.ScenarioTrace;
import aggregate_logging.ALogging;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.List;

public class StepWiseExecutor extends EntityCore<Simulation>
        implements SimulationExecutor, EntityProxy<StepWiseExecutor> {

    protected static final String STEPS_PARAM = "steps";
    /** Optional real-time pacing: minimum duration of a step, in milliseconds (0 = run as fast as possible). */
    protected static final String STEP_PERIOD_PARAM = "stepPeriod";
    int nSteps;
    long stepPeriodMs;
    Thread executor;
    Simulation simulation;

    @Override
    public String getEntityName() {
        return getName();
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        nSteps = configuration.containsKey(STEPS_PARAM) ? Integer.parseInt(configuration.getAValue(STEPS_PARAM)) : 100;
        stepPeriodMs = configuration.containsKey(STEP_PERIOD_PARAM)
                ? Long.parseLong(configuration.getAValue(STEP_PERIOD_PARAM)) : 0;
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
        if (!(context instanceof Simulation))
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

        for (Entity<?> entity : new ArrayList<>(simulation.getSimulationObjects()))
            if (entity instanceof SteppableEntity && !entity.isRunning())
                ((SteppableEntity) entity).startSuspended();

        executor = new Thread() {
            @Override
            public void run() {
                for (long step = 0; step < nSteps; step++) {
                    runStep(step);
                    if (stepPeriodMs > 0)
                        try {
                            Thread.sleep(stepPeriodMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                }
                simulation.executionCompleted();
                ALogging.getInstance().printAllAgr();
                ScenarioTrace.exportRun();
            }
        };
        executor.start();
        return true;
    }

    protected void runStep(long step) {
        li("Step []", Long.valueOf(step));
        ScenarioTrace.setStep(step);
        // Iterate over a copy to avoid ConcurrentModificationException when entities deregister during the step.
        List<Entity<?>> snapshot = new ArrayList<>(simulation.getSimulationObjects());
        simulation.clearDeregistered();
        for (Entity<?> entity : snapshot) {
            // Check if entity is still registered before sending events
            if (simulation.isDeregistered(entity))
                continue;
            // Push pending events to the entity from all contexts
            for (SimulationContext context : simulation.getSimulationContexts()) {
                context.sendEvents(entity);
                // If the entity deregistered during event processing, stop
                if (simulation.isDeregistered(entity))
                    break;
            }
            // Check again after all events have been delivered
            if (simulation.isDeregistered(entity))
                continue;
            if (entity instanceof SteppableEntity)
                ((SteppableEntity) entity).step();
            else if (entity instanceof Patch)
                ((Patch) entity).step();
        }
        // All entities have been stepped, now update the simulation contexts
        for (SimulationContext context : simulation.getSimulationContexts())
            context.validateAndExecutePendingActions();
        simulation.stepCompleted();
    }

    @Override
    public boolean stop() {
        try {
            executor.join();
        } catch (InterruptedException e) {
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
