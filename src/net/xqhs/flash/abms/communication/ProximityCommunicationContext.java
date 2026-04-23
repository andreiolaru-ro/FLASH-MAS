package net.xqhs.flash.abms.communication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;

import net.xqhs.flash.abms.Patch;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.ShardContainer;

public class ProximityCommunicationContext extends SimulationContext.BaseContext implements EntityProxy<ProximityCommunicationContext> {

    protected static class BroadcastRecord {
        final EntityProxy<?> sender;
        final Position senderPosition;
        final AgentWave wave;

        BroadcastRecord(EntityProxy<?> sender, Position senderPosition, AgentWave wave) {
            this.sender = sender;
            this.senderPosition = senderPosition;
            this.wave = wave;
        }
    }

    protected static class TargetedWaveRecord {
        final EntityProxy<?> target;
        final AgentWave wave;

        TargetedWaveRecord(EntityProxy<?> target, AgentWave wave) {
            this.target = target;
            this.wave = wave;
        }
    }

    protected Queue<BroadcastRecord> pendingBroadcasts = new LinkedList<>();
    protected Queue<TargetedWaveRecord> pendingTargetedWaves = new LinkedList<>();
    protected Map<String, List<AgentWave>> pendingWaveEvents = new HashMap<>();
    protected SpaceContext space = null;

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof SpaceContext)
            space = (SpaceContext) context;
        return super.addGeneralContext(context);
    }

    public boolean broadcast(EntityProxy<?> sender, AgentWave wave) {
        if (space == null) {
            le("No space context; broadcast dropped.");
            return false;
        }
        Position senderPosition = space.getPosition(sender);
        if (senderPosition == null)
            return false;
        pendingBroadcasts.add(new BroadcastRecord(sender, senderPosition, wave));
        return true;
    }

    public boolean sendWaveTo(EntityProxy<?> target, AgentWave wave) {
        if (target == null || target.getEntityName() == null)
            return false;
        pendingTargetedWaves.add(new TargetedWaveRecord(target, wave));
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void validateAndExecutePendingActions() {
        // Resolve broadcasts to targeted deliveries
        for (BroadcastRecord broadcastRecord : pendingBroadcasts) {
            Set<Position> vicinity = space.getVicinity(broadcastRecord.senderPosition);
            for (Position vpos : vicinity) {
                for (EntityProxy<?> entity : (Set<EntityProxy<?>>) space.getEntitiesAt(vpos)) {
                    if ((entity instanceof ShardContainer || entity instanceof Patch)
                            && entity.getEntityName() != null)
                        pendingWaveEvents.computeIfAbsent(entity.getEntityName(), k -> new ArrayList<>())
                                .add(broadcastRecord.wave);
                }
            }
        }
        pendingBroadcasts.clear();

        // Resolve targeted waves
        for (TargetedWaveRecord record : pendingTargetedWaves) {
            String targetName = record.target.getEntityName();
            if (targetName != null)
                pendingWaveEvents.computeIfAbsent(targetName, k -> new ArrayList<>()).add(record.wave);
        }
        pendingTargetedWaves.clear();
    }

    @Override
    public void sendEvents(Entity<?> entity) {
        if (entity == null)
            return;
        String recipientKey = entity.getName();
        if (recipientKey == null && entity instanceof EntityProxy<?>)
            recipientKey = ((EntityProxy<?>) entity).getEntityName();
        if (recipientKey == null)
            return;
        List<AgentWave> waves = pendingWaveEvents.get(recipientKey);
        if (waves == null || waves.isEmpty())
            return;

        // Deliver to ShardContainer (agents) or Patch entities
        if (entity instanceof ShardContainer) {
            ShardContainer container = (ShardContainer) entity;
            Iterator<AgentWave> it = waves.iterator();
            while (it.hasNext()) {
                if (container.postAgentEvent(it.next()))
                    it.remove();
            }
        } else if (entity instanceof Patch) {
            Patch patch = (Patch) entity;
            Iterator<AgentWave> it = waves.iterator();
            while (it.hasNext()) {
                if (patch.postAgentEvent(it.next()))
                    it.remove();
            }
        }

        if (waves.isEmpty())
            pendingWaveEvents.remove(recipientKey);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Simulation>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public String getEntityName() {
        return name != null ? name : "ProximityCommunication";
    }

}
