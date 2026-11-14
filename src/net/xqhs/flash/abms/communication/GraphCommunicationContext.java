package net.xqhs.flash.abms.communication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.xqhs.flash.abms.Patch;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.graph.GraphPosition;
import net.xqhs.flash.abms.space.graph.GraphTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.ShardContainer;

public class GraphCommunicationContext extends CommunicationContext
		implements EntityProxy<GraphCommunicationContext> {

	protected static class InFlightMessage {
		final AgentWave wave;
		final List<GraphPosition> path;
		int currentHop;

		InFlightMessage(AgentWave wave, List<GraphPosition> path) {
			this.wave = wave;
			this.path = path;
			this.currentHop = 0;
		}

		GraphPosition currentPosition() {
			return path.get(currentHop);
		}

		boolean advance() {
			currentHop++;
			return currentHop >= path.size() - 1;
		}

		GraphPosition destination() {
			return path.get(path.size() - 1);
		}

		String destinationNodeId() {
			return destination().getNodeId();
		}
	}

	private final Queue<InFlightMessage> inFlightMessages = new LinkedList<>();
	private final Queue<AgentWave> pendingBroadcasts = new LinkedList<>();
	private final Queue<GraphPosition> broadcastOrigins = new LinkedList<>();

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return super.addGeneralContext(context);
	}

	@Override
	public boolean broadcast(EntityProxy<?> sender, AgentWave wave) {
		if (space == null) {
			le("No space context; broadcast dropped.");
			return false;
		}
		Position senderPosition = getPositionRaw(sender);
		if (!(senderPosition instanceof GraphPosition))
			return false;
		pendingBroadcasts.add(wave);
		broadcastOrigins.add((GraphPosition) senderPosition);
		return true;
	}

	@Override
	public boolean sendWaveTo(EntityProxy<?> target, AgentWave wave) {
		// Default: try to find sender by looking up all entities (slow fallback)
		return sendWaveFromTo(null, target, wave);
	}

	public boolean sendWaveFromTo(EntityProxy<?> sender, EntityProxy<?> target, AgentWave wave) {
		if (space == null || target == null || target.getEntityName() == null)
			return false;
		if (!(space.getTopology() instanceof GraphTopology))
			return false;

		GraphTopology topology = (GraphTopology) space.getTopology();
		GraphPosition targetPos = (GraphPosition) getPositionRaw(target);
		GraphPosition senderPos = sender != null ? (GraphPosition) getPositionRaw(sender) : null;

		if (targetPos == null || senderPos == null) {
			deliverToEntity(target.getEntityName(), wave);
			return true;
		}

		if (senderPos.equals(targetPos)) {
			deliverToEntity(target.getEntityName(), wave);
			return true;
		}

		List<GraphPosition> path = topology.findPathAStar(senderPos, targetPos);
		if (path.isEmpty()) {
			le("No path from [] to []", senderPos, targetPos);
			return false;
		}

		inFlightMessages.add(new InFlightMessage(wave, path));
		return true;
	}

	/**
	 * Sends a message directly to a target, bypassing graph routing.
	 * Used for PersonAgent-to-AuctionAgent communication (off-graph entities).
	 */
	public boolean sendDirect(EntityProxy<?> target, AgentWave wave) {
		if (target == null || target.getEntityName() == null)
			return false;
		deliverToEntity(target.getEntityName(), wave);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void validateAndExecutePendingActions() {
		// Process broadcasts — deliver to immediate graph neighbors
		while (!pendingBroadcasts.isEmpty()) {
			AgentWave wave = pendingBroadcasts.poll();
			GraphPosition origin = broadcastOrigins.poll();
			Set<Position> vicinity = getVicinityRaw(origin);
			for (Position vpos : vicinity) {
				for (EntityProxy<?> entity : getEntitiesAtRaw(vpos)) {
					if ((entity instanceof ShardContainer || entity instanceof Patch)
							&& entity.getEntityName() != null)
						deliverToEntity(entity.getEntityName(), wave);
				}
			}
		}

		// Advance in-flight messages by one hop
		List<InFlightMessage> arrived = new ArrayList<>();
		for (InFlightMessage msg : inFlightMessages) {
			boolean reachedDestination = msg.advance();
			if (reachedDestination) {
				Set<EntityProxy<?>> entitiesAtDest = getEntitiesAtRaw(msg.destination());
				for (EntityProxy<?> entity : entitiesAtDest) {
					if ((entity instanceof ShardContainer || entity instanceof Patch)
							&& entity.getEntityName() != null)
						deliverToEntity(entity.getEntityName(), msg.wave);
				}
				arrived.add(msg);
			}
		}
		inFlightMessages.removeAll(arrived);
	}

	@Override
	public String getEntityName() {
		return name != null ? name : "GraphCommunication";
	}

}
