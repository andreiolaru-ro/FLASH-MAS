package net.xqhs.flash.abms;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.xqhs.flash.abms.AgentManagementContext.AgentManagementActionData;
import net.xqhs.flash.abms.SimulationContext.ActionRecord;
import net.xqhs.flash.abms.SimulationContext.BaseContext.BaseActionData;
import net.xqhs.flash.abms.communication.CommunicationContext;
import net.xqhs.flash.abms.communication.GraphCommunicationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.SpaceContext.SpaceActionData;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.core.util.PlatformUtils;


public class EnvironmentLinkShard extends AgentShardCore {

    protected static final String SHARD_NAME = "Environment";

    SpaceContext space = null;
    CommunicationContext communication = null;
    AgentManagementContext agentManagement = null;
    RandomContext randomContext = null;
    TemporalContext temporal = null;
    Simulation simulation = null;
    MessagingShard messaging = null;
    /** Calls the agent's step when the agent steps by itself; <code>null</code> otherwise. */
    ScheduledExecutorService stepper = null;

    public EnvironmentLinkShard() {
        super(AgentShardDesignation.customShard(SHARD_NAME));
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof SpaceContext)
            space = (SpaceContext) context;
        else if (context instanceof AgentManagementContext)
            agentManagement = (AgentManagementContext) context;
        else if (context instanceof RandomContext)
            randomContext = (RandomContext) context;
        else if (context instanceof TemporalContext)
            temporal = (TemporalContext) context;
        else if (context instanceof CommunicationContext)
            communication = (CommunicationContext) context;
        else if (context instanceof Simulation)
            simulation = (Simulation) context;
        else if (context instanceof PylonProxy && messaging == null && getAgent() != null)
            messaging = (MessagingShard) instantiateRecommendedShard(
                    AgentShardDesignation.StandardAgentShard.MESSAGING, (PylonProxy) context, null, getAgent());
        if (!super.addGeneralContext(context))
            return false;

        if (agentManagement != null && getContext() != null)
            agentManagement.registerAgent(getContext(), this);
        // FIXME should actually be *closest* context
        return true;
    }

    public Position getCurrentPosition() {
        if (space == null)
            return null;
        return space.getPosition(getContext());
    }

    public Set<Position> getVicinity(Position pos) {
        if (space == null)
            return Collections.emptySet();
        return space.getVicinity(pos);
    }

    public Set<Position> getValidNeighborPositions(Position pos) {
        if (space == null)
            return Collections.emptySet();
        return space.getValidNeighborPositions(pos);
    }

    public boolean moveToPosition(Position target) {
        if (space == null)
            return false;
        return space.addPendingAction(new ActionRecord(getContext(),
                new MultiValueMap()
                        .add(BaseActionData.ACTION.s(), SpaceActionData.MOVE_ACTION.s())
                        .addObject(SpaceActionData.MOVE_TARGET.s(), target)));
    }

    public Set<EntityProxy<?>> getEntitiesAt(Position pos) {
        if (space == null)
            return Collections.emptySet();
        return space.getEntitiesAt(pos);
    }

    public Map<Position, Set<EntityProxy<?>>> observe(int range) {
        if (space == null)
            return Collections.emptyMap();
        return space.getEntitiesWithinRange(getCurrentPosition(), range);
    }

    public Set<EntityProxy<?>> getAllEntities() {
        if (space == null)
            return Collections.emptySet();
        return space.getAllEntities();
    }

    public Topology<? extends Position> getTopology() {
        if (space == null)
            return null;
        return space.getTopology();
    }

    public boolean requestDestroyAgent(EntityProxy<?> target) {
        return agentManagement.addPendingAction(new ActionRecord(getContext(),
                new MultiValueMap()
                        .add(BaseActionData.ACTION.s(), AgentManagementActionData.DESTROY_ACTION.s())
                        .addObject(AgentManagementActionData.DESTROY_TARGET.s(), target)));
    }

    public void notifyAgentDestroyed() {
        // no-op: destruction is now handled via events and self-deregistration
    }

    public boolean broadcast(AgentWave wave) {
        if (communication == null)
            return false;
        return communication.broadcast(getContext(), wave);
    }

    public boolean sendWaveTo(EntityProxy<?> target, AgentWave wave) {
        if (communication == null)
            return false;
        if (communication instanceof GraphCommunicationContext)
            return ((GraphCommunicationContext) communication).sendWaveFromTo(getContext(), target, wave);
        return communication.sendWaveTo(target, wave);
    }

    /**
     * Makes the agent call its step by itself, every <code>periodMs</code> milliseconds, on a thread of its own.     * @param step
     *            - the step of the agent. Must pair with stopStepping().
     * @param periodMs
     *            - the time between the end of a step and the start of the next one.
     * @return <code>true</code> if stepping was started.
     */
    public synchronized boolean startStepping(Runnable step, long periodMs) {
        if (stepper != null)
            return false;
        String agentName = getAgent() != null ? getAgent().getEntityName() : SHARD_NAME;
        stepper = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, agentName + "-stepper"));
        stepper.scheduleWithFixedDelay(() -> {
            try {
                step.run();
            } catch (RuntimeException ex) {
                le("Step failed: []", PlatformUtils.printException(ex));
            }
        }, 0, Math.max(1, periodMs), TimeUnit.MILLISECONDS);
        return true;
    }

    public synchronized void stopStepping() {
        if (stepper == null)
            return;
        stepper.shutdownNow();
        stepper = null;
    }

    public boolean schedule(long time, AgentEvent event) {
        if (temporal == null)
            return false;
        return temporal.createTask(time, getAgent(), event);
    }

    public long getCurrentTime() {
        return temporal == null ? -1 : temporal.getCurrentTime();
    }

    public MessagingShard getMessagingShard() {
        return messaging;
    }

    public boolean sendTo(String targetName, AgentWave wave) {
        if (messaging != null)
            return messaging.sendMessage(messaging.getAgentAddress(), targetName, wave.getSerializedContent());
        EntityProxy<?> target = findByName(targetName);
        return target != null && sendWaveTo(target, wave);
    }

    public boolean sendDirect(String targetName, AgentWave wave) {
        if (messaging != null)
            return messaging.sendMessage(messaging.getAgentAddress(), targetName, wave.getSerializedContent());
        EntityProxy<?> target = findByName(targetName);
        if (target == null)
            return false;
        if (communication instanceof GraphCommunicationContext)
            return ((GraphCommunicationContext) communication).sendDirect(target, wave);
        return sendWaveTo(target, wave);
    }

    protected EntityProxy<?> findByName(String entityName) {
        if (entityName == null || simulation == null)
            return null;
        for (Entity<?> entity : simulation.getSimulationObjects())
            if (entityName.equals(entity.asContext().getEntityName()))
                return entity.asContext();
        return null;
    }

    @SuppressWarnings("unchecked")
    public Set<EntityProxy<?>> getEntitiesInVicinity() {
        if (space == null || getContext() == null)
            return Collections.emptySet();
        Position pos = getCurrentPosition();
        if (pos == null)
            return Collections.emptySet();
        java.util.HashSet<EntityProxy<?>> result = new java.util.HashSet<>();
        for (Position vpos : (Set<Position>) space.getVicinity(pos))
            result.addAll(space.getEntitiesAt(vpos));
        return result;
    }

    public int nextInt(int bound) {
        return randomContext.nextInt(bound);
    }

    public double nextDouble() {
        return randomContext.nextDouble();
    }

    public boolean nextBoolean() {
        return randomContext.nextBoolean();
    }

    public double nextGaussian() {
        return randomContext.nextGaussian();
    }
}
