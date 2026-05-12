package net.xqhs.flash.abms.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Set;

import net.xqhs.flash.abms.Patch;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.ShardContainer;

public abstract class CommunicationContext extends SimulationContext.BaseContext {

	protected Map<String, List<AgentWave>> pendingWaveEvents = new HashMap<>();
	protected SpaceContext<?> space = null;

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if (context instanceof SpaceContext)
			space = (SpaceContext<?>) context;
		return super.addGeneralContext(context);
	}

	public abstract boolean broadcast(EntityProxy<?> sender, AgentWave wave);

	public abstract boolean sendWaveTo(EntityProxy<?> target, AgentWave wave);

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
		return (EntityProxy<C>) this;
	}
}
