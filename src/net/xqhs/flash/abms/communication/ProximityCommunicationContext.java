package net.xqhs.flash.abms.communication;

import java.util.Set;

import net.xqhs.flash.abms.Patch;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiValueMap;

public class ProximityCommunicationContext extends CommunicationContext {

	@Override
	public boolean broadcast(EntityProxy<?> sender, AgentWave wave) {
		if (space == null) {
			le("No space context; broadcast dropped.");
			return false;
		}
		Position senderPosition = getPositionRaw(sender);
		if (senderPosition == null)
			return false;
		return addPendingAction(new ActionRecord(sender,
				new MultiValueMap().add(BaseActionData.ACTION.s(), CommunicationActionData.BROADCAST_ACTION.s())
						.addObject(CommunicationActionData.WAVE.s(), wave)
						.addObject(CommunicationActionData.SENDER_POSITION.s(), senderPosition)));
	}

	@Override
	public boolean send(AgentWave wave) {
		if (wave.getFirstDestinationElement() == null)
			return false;
		return addPendingAction(new ActionRecord(null,
				new MultiValueMap().add(BaseActionData.ACTION.s(), CommunicationActionData.SEND_ACTION.s())
						.addObject(CommunicationActionData.WAVE.s(), wave)));
	}

	@Override
	public void validateAndExecutePendingActions() {
		for (ActionRecord action : pendingActions) {
			if (!CommunicationActionData.BROADCAST_ACTION.s().equals(action.getActionData().get(BaseActionData.ACTION.s())))
				continue;
			AgentWave wave = (AgentWave) action.getActionData().getObject(CommunicationActionData.WAVE.s());
			Position senderPosition = (Position) action.getActionData()
					.getObject(CommunicationActionData.SENDER_POSITION.s());
			Set<Position> vicinity = getVicinityRaw(senderPosition);
			for (Position vpos : vicinity) {
				for (EntityProxy<?> entity : getEntitiesAtRaw(vpos)) {
					if ((entity instanceof ShardContainer || entity instanceof Patch)
							&& entity.getEntityName() != null)
						deliverToEntity(entity.getEntityName(), wave);
				}
			}
		}
		for (ActionRecord action : pendingActions) {
			if (!CommunicationActionData.SEND_ACTION.s().equals(action.getActionData().get(BaseActionData.ACTION.s())))
				continue;
			AgentWave wave = (AgentWave) action.getActionData().getObject(CommunicationActionData.WAVE.s());
			deliverToEntity(wave.getFirstDestinationElement(), wave);
		}
		pendingActions.clear();
	}

	@Override
	public String getEntityName() {
		return name != null ? name : "ProximityCommunication";
	}
}
