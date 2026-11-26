package net.xqhs.flash.abms.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;

public abstract class CommunicationContext extends SimulationContext.BaseContext implements WaveMessagingPylonProxy {

	public enum CommunicationActionData implements ActionData {
		BROADCAST_ACTION, SEND_ACTION, WAVE, SENDER_POSITION;

		@Override
		public String s() {
			return this.toString();
		}
	}

	protected Map<String, List<AgentWave>> pendingWaveEvents = new HashMap<>();
	protected SpaceContext<?> space = null;
	protected Simulation simulation = null;

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if (context instanceof SpaceContext)
			space = (SpaceContext<?>) context;
		if (context instanceof Simulation)
			simulation = (Simulation) context;
		return super.addGeneralContext(context);
	}

	public abstract boolean broadcast(EntityProxy<?> sender, AgentWave wave);

	@Override
	public abstract boolean send(AgentWave wave);

	@Override
	public boolean register(String entityName, WaveReceiver receiver) {
		return true;
	}

	@Override
	public boolean unregister(String entityName, WaveReceiver registeredReceiver) {
		return false;
	}

	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
		return null;
	}

	protected EntityProxy<?> findByName(String entityName) {
		if (entityName == null || simulation == null)
			return null;
		for (Entity<?> entity : simulation.getSimulationObjects())
			if (entityName.equals(entity.getName()))
				return entity.asContext();
		return null;
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

		List<AgentWave> waves = pendingWaveEvents.remove(recipientKey);
		if (waves == null)
			return;
		for (AgentWave wave : waves) {
			if (entity instanceof ShardContainer)
				((ShardContainer) entity).postAgentEvent(wave);
		}
	}

	protected void deliverToEntity(String entityName, AgentWave wave) {
		pendingWaveEvents.computeIfAbsent(entityName, k -> new ArrayList<>()).add(wave);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Set<Position> getVicinityRaw(Position pos) {
		return ((SpaceContext) space).getVicinity(pos);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Set<EntityProxy<?>> getEntitiesAtRaw(Position pos) {
		return ((SpaceContext) space).getEntitiesAt(pos);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Position getPositionRaw(EntityProxy<?> entity) {
		return (Position) ((SpaceContext) space).getPosition(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C extends Entity<Simulation>> EntityProxy<C> asContext() {
		return (EntityProxy<C>) (Object) this;
	}
}
