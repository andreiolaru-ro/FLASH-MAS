package net.xqhs.flash.abms.communication;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;

public class GeneralCommunicationContext extends CommunicationContext
		implements WaveMessagingPylonProxy {

	protected Queue<AgentWave>	pendingWaves	= new LinkedList<>();
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
		pendingWaves.add(wave);
		return true;
	}

	@Override
	public void validateAndExecutePendingActions() {
		for(AgentWave wave : pendingWaves) {
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
		pendingWaves.clear();
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
