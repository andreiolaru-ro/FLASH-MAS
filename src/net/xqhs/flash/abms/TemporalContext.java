package net.xqhs.flash.abms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;

public class TemporalContext extends SimulationContext.BaseContext
        implements SimulationContext, EntityProxy<TemporalContext> {

    /** Parameter: duration of a time unit, in milliseconds, in deployment mode. */
    protected static final String MULTIPLIER_PARAM = "multiplier";
    protected static final long DEFAULT_MULTIPLIER_MS = 100;

    protected long multiplierMs = DEFAULT_MULTIPLIER_MS;
    protected Simulation simulation = null;

    protected static final String SCHEDULE_ACTION = "SCHEDULE_ACTION";

    protected static class ScheduledTask extends ActionRecord {
        final long time;
        final ShardContainer target;
        final AgentEvent event;

        ScheduledTask(long time, ShardContainer target, AgentEvent event) {
            super(target, new MultiValueMap().add(BaseActionData.ACTION.s(), SCHEDULE_ACTION));
            this.time = time;
            this.target = target;
            this.event = event;
        }
    }

    /** Simulation mode: the current step. */
    protected long currentStep = 0;

    protected final Map<ShardContainer, List<ScheduledTask>> tasks = new LinkedHashMap<>();

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
        if (simulation != null)
            return addPendingAction(new ScheduledTask(time, target, event));
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
        List<ScheduledTask> dueTasks = new ArrayList<>();
        synchronized (this) {
            List<ScheduledTask> entityTasks = tasks.get(entity.asContext());
            if (entityTasks == null)
                return;
            for (Iterator<ScheduledTask> it = entityTasks.iterator(); it.hasNext();) {
                ScheduledTask task = it.next();
                if (task.time <= currentStep) {
                    dueTasks.add(task);
                    it.remove();
                }
            }
            if (entityTasks.isEmpty())
                tasks.remove(entity.asContext());
        }
        // delivered outside the lock, since the entity may create new tasks while processing the event
        for (ScheduledTask task : dueTasks)
            task.target.postAgentEvent(task.event);
    }


    @Override
    public synchronized void validateAndExecutePendingActions() {
        for (ActionRecord action : pendingActions) {
            ScheduledTask task = (ScheduledTask) action;
            tasks.computeIfAbsent(task.target, t -> new ArrayList<>()).add(task);
        }
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
