package net.xqhs.flash.abms.communication;

import java.util.Optional;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiValueMap;

public class GeneralCommunicationContext extends CommunicationContext
		implements WaveMessagingPylonProxy {

	protected Simulation		simulation;

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(context instanceof Simulation)
			simulation = (Simulation) context;
		return super.addGeneralContext(context);
	}
	
	@Override
	public boolean broadcast(EntityProxy<?> sender, AgentWave wave) {
		return false;
	}

	@Override
	public boolean sendWaveTo(EntityProxy<?> target, AgentWave wave) {
		String to = wave.getFirstDestinationElement();
		if(to == null || !simulation.getSimulationObjects().stream().filter(e -> to.equals(e.getName())).findFirst()
				.isPresent())
			return ler(false, "Target empty or not found in wave", wave);
		return addPendingAction(new ActionRecord(null,
				new MultiValueMap().add(BaseActionData.ACTION.s(), CommunicationActionData.SEND_ACTION.s())
						.addObject(CommunicationActionData.WAVE.s(), wave)));
	}

	@Override
	public void validateAndExecutePendingActions() {
		for(ActionRecord action : pendingActions) {
			AgentWave wave = (AgentWave) action.getActionData().getObject(CommunicationActionData.WAVE.s());
			String to = wave.getFirstDestinationElement();
			if(to == null) {
				lw("Empty target in wave", wave);
				continue;
			}
			Optional<Entity<?>> target = simulation.getSimulationObjects().stream().filter(e -> to.equals(e.getName()))
					.findFirst();
			if(target.isPresent())
				deliverToEntity(to, wave);
			else
				lw("Message target disappeared", to);
		}
		pendingActions.clear();
	}

	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
		return null;
	}
	
	@Override
	public String getEntityName() {
		return name != null ? name : "ProximityCommunication";
	}
	
	@Override
	public boolean register(String entityName, WaveReceiver receiver) {
		// all simulation objects are registered anyway
		return true;
	}
	
	@Override
	public boolean unregister(String entityName, WaveReceiver registeredReceiver) {
		// unregistration is not possible
		return false;
	}
	
	@Override
	public boolean send(AgentWave wave) {
		return sendWaveTo(null, wave);
	}
	
}
