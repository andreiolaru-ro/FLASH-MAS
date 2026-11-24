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
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;

public class GraphCommunicationContext extends CommunicationContext {

	/**
	 * When present (and not <code>false</code>), makes waves between entities placed in the graph
	 * travel along the graph; otherwise, waves are delivered directly, at the next step.
	 */
	public static final String ROUTING_PARAM = "routing";

	protected boolean routing = false;

	@Override
	public boolean configure(MultiTreeMap configuration) {
		if (!super.configure(configuration))
			return false;
		if (configuration != null && configuration.containsKey(ROUTING_PARAM)) {
			String value = configuration.getAValue(ROUTING_PARAM);
			routing = value == null || !Boolean.FALSE.toString().equals(value);
		}
		return true;
	}

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
		return addPendingAction(new ActionRecord(sender,
				new MultiValueMap().add(BaseActionData.ACTION.s(), CommunicationActionData.BROADCAST_ACTION.s())
						.addObject(CommunicationActionData.WAVE.s(), wave)
						.addObject(CommunicationActionData.SENDER_POSITION.s(), senderPosition)));
	}

	@Override
	public boolean send(AgentWave wave) {
		String targetName = wave.getFirstDestinationElement();
		if (targetName == null)
			return false;
		if (!routing) {
			deliverToEntity(targetName, wave);
			return true;
		}
		if (space == null)
			return false;
		if (!(space.getTopology() instanceof GraphTopology))
			return false;

		GraphTopology topology = (GraphTopology) space.getTopology();
		EntityProxy<?> target = findByName(targetName);
		EntityProxy<?> sender = findByName(wave.getFirstSource());
		GraphPosition targetPos = target != null ? (GraphPosition) getPositionRaw(target) : null;
		GraphPosition senderPos = sender != null ? (GraphPosition) getPositionRaw(sender) : null;

		if (targetPos == null || senderPos == null || senderPos.equals(targetPos)) {
			deliverToEntity(targetName, wave);
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

	@SuppressWarnings("unchecked")
	@Override
	public void validateAndExecutePendingActions() {
		// Process broadcasts — deliver to immediate graph neighbors
		for (ActionRecord action : pendingActions) {
			AgentWave wave = (AgentWave) action.getActionData().getObject(CommunicationActionData.WAVE.s());
			GraphPosition origin = (GraphPosition) action.getActionData()
					.getObject(CommunicationActionData.SENDER_POSITION.s());
			Set<Position> vicinity = getVicinityRaw(origin);
			for (Position vpos : vicinity) {
				for (EntityProxy<?> entity : getEntitiesAtRaw(vpos)) {
					if ((entity instanceof ShardContainer || entity instanceof Patch)
							&& entity.getEntityName() != null)
						deliverToEntity(entity.getEntityName(), wave);
				}
			}
		}

		pendingActions.clear();

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
