package net.xqhs.flash.abms.communication;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import net.xqhs.flash.abms.Patch;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.ShardContainer;

public class ProximityCommunicationContext extends CommunicationContext
		implements EntityProxy<ProximityCommunicationContext> {

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

	@Override
	public boolean broadcast(EntityProxy<?> sender, AgentWave wave) {
		if (space == null) {
			le("No space context; broadcast dropped.");
			return false;
		}
		Position senderPosition = getPositionRaw(sender);
		if (senderPosition == null)
			return false;
		pendingBroadcasts.add(new BroadcastRecord(sender, senderPosition, wave));
		return true;
	}

	@Override
	public boolean sendWaveTo(EntityProxy<?> target, AgentWave wave) {
		if (target == null || target.getEntityName() == null)
			return false;
		pendingTargetedWaves.add(new TargetedWaveRecord(target, wave));
		return true;
	}

	@Override
	public void validateAndExecutePendingActions() {
		for (BroadcastRecord broadcastRecord : pendingBroadcasts) {
			Set<Position> vicinity = getVicinityRaw(broadcastRecord.senderPosition);
			for (Position vpos : vicinity) {
				for (EntityProxy<?> entity : getEntitiesAtRaw(vpos)) {
					if ((entity instanceof ShardContainer || entity instanceof Patch)
							&& entity.getEntityName() != null)
						deliverToEntity(entity.getEntityName(), broadcastRecord.wave);
				}
			}
		}
		pendingBroadcasts.clear();

		for (TargetedWaveRecord record : pendingTargetedWaves) {
			String targetName = record.target.getEntityName();
			if (targetName != null)
				deliverToEntity(targetName, record.wave);
		}
		pendingTargetedWaves.clear();
	}

	@Override
	public String getEntityName() {
		return name != null ? name : "ProximityCommunication";
	}
}
