package net.xqhs.flash.abms;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;

public class TemporalContext extends SimulationContext.BaseContext
        implements SimulationContext, EntityProxy<TemporalContext> {

    /** Parameter: duration of a time unit, in milliseconds, in deployment mode. */
    protected static final String MULTIPLIER_PARAM = "multiplier";
    protected static final long DEFAULT_MULTIPLIER_MS = 100;

    protected long multiplierMs = DEFAULT_MULTIPLIER_MS;
    protected Simulation simulation = null;

    /** Simulation mode: the current step. */
    protected long currentStep = 0;
    /** Simulation mode: for each task not yet delivered, the step at which to deliver it. */
    protected final List<Long> taskSteps = new ArrayList<>();
    /** Simulation mode: for each task not yet delivered, the entity to deliver it to. */
    protected final List<ShardContainer> taskTargets = new ArrayList<>();
    /** Simulation mode: for each task not yet delivered, the event to deliver. */
    protected final List<AgentEvent> taskEvents = new ArrayList<>();

    /** Deployment mode: the moment (ms) when the context started. */
    protected long startMillis = System.currentTimeMillis();
    /** Deployment mode: delivers the events; created on first use, cancelled on stop. */
    protected Timer timer = null;

    public TemporalContext(MultiTreeMap configuration) {
        configure(configuration);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        super.configure(configuration);
        if (configuration != null && configuration.containsKey(MULTIPLIER_PARAM))
            multiplierMs = Long.parseLong(configuration.getAValue(MULTIPLIER_PARAM));
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        return super.addGeneralContext(context);
    }

    @Override
    public boolean start() {
        startMillis = System.currentTimeMillis();
        return super.start();
    }

    @Override
    public synchronized boolean stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        return super.stop();
    }

    public synchronized long getCurrentTime() {
        if (simulation != null)
            return currentStep;
        return (System.currentTimeMillis() - startMillis) / multiplierMs;
    }

    public synchronized boolean createTask(long time, ShardContainer target, AgentEvent event) {
        if (target == null || event == null)
            return false;
        if (simulation != null) {
            taskSteps.add(Long.valueOf(time));
            taskTargets.add(target);
            taskEvents.add(event);
            return true;
        }
        if (timer == null)
            timer = new Timer(getEntityName() + "-timer");
        long deliveryMillis = multiplierMs * time;
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        long delay = Math.max(0, deliveryMillis - elapsedMillis);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                target.postAgentEvent(event);
            }
        }, delay);
        return true;
    }

    @Override
    public void sendEvents(Entity<?> entity) {
        Object entityContext = entity.asContext();
        ShardContainer target = null;
        List<AgentEvent> dueEvents = new ArrayList<>();
        synchronized (this) {
            int i = 0;
            while (i < taskSteps.size()) {
                if (taskSteps.get(i).longValue() <= currentStep && taskTargets.get(i) == entityContext) {
                    target = taskTargets.remove(i);
                    dueEvents.add(taskEvents.remove(i));
                    taskSteps.remove(i);
                }
                else
                    i++;
            }
        }
        // delivered outside the lock, since the entity may create new tasks while processing the event
        for (AgentEvent event : dueEvents)
            target.postAgentEvent(event);
    }


    @Override
    public synchronized void validateAndExecutePendingActions() {
        pendingActions.clear();
        currentStep++;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Simulation>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public String getEntityName() {
        return name;
    }
}
