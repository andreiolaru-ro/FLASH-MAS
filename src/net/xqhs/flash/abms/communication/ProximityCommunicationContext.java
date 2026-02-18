package net.xqhs.flash.abms.communication;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.WaveReceiver;

public class ProximityCommunicationContext extends SimulationContext.BaseContext implements EntityProxy<ProximityCommunicationContext> {

    protected static class BroadcastRecord {
        final EntityProxy<?> sender;
        final AgentWave wave;

        BroadcastRecord(EntityProxy<?> sender, AgentWave wave) {
            this.sender = sender;
            this.wave = wave;
        }
    }

    protected Map<EntityProxy<?>, WaveReceiver> receivers = new HashMap<>();
    protected Queue<BroadcastRecord> pendingBroadcasts = new LinkedList<>();
    protected SpaceContext space = null;

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof SpaceContext)
            space = (SpaceContext) context;
        return super.addGeneralContext(context);
    }

    public boolean register(EntityProxy<?> entity, WaveReceiver receiver) {
        receivers.put(entity, receiver);
        return true;
    }

    public boolean unregister(EntityProxy<?> entity, WaveReceiver registeredReceiver) {
        return receivers.remove(entity, registeredReceiver);
    }

    public boolean broadcast(EntityProxy<?> sender, AgentWave wave) {
        if (space == null) {
            le("No space context; broadcast dropped.");
            return false;
        }
        pendingBroadcasts.add(new BroadcastRecord(sender, wave));
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void validateAndExecutePendingActions() {
        for (BroadcastRecord broadcastRecord : pendingBroadcasts) {
            Position senderPosition = space.getPosition(broadcastRecord.sender);
            if (senderPosition == null) {
                le("Sender [] has no position; broadcast dropped.", broadcastRecord.sender.getEntityName());
                continue;
            }
            Set<Position> vicinity = space.getVicinity(senderPosition);
            for (Position vpos : vicinity) {
                for (EntityProxy<?> entity : (Set<EntityProxy<?>>) space.getEntitiesAt(vpos)) {
                    WaveReceiver receiver = receivers.get(entity);
                    if (receiver != null)
                        receiver.receive(broadcastRecord.wave);
                }
            }
        }
        pendingBroadcasts.clear();
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
