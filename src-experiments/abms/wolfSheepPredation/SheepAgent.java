package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.support.Pylon;

public class SheepAgent extends BaseAgent implements SteppableEntity, EntityProxy<BaseAgent> {
	
	protected EnvironmentLinkShard	e		= new EnvironmentLinkShard();
	protected final Random			random	= new Random();
	protected int					visionRange	= 2;

	public void setVisionRange(int visionRange) {
		this.visionRange = visionRange;
	}
	
	public SheepAgent() {
		e.addGeneralContext(this);
		e.addContext(new BaseAgentProxy() {
			@Override
			public boolean postAgentEvent(AgentEvent event) {
				if(event.getType() == AgentEventType.AGENT_STOP)
					stop();
				return true;
			}
		});
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		return (EntityProxy<C>) this;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		e.addGeneralContext(context);
		return super.addGeneralContext(context);
	}
	
	@Override
	public void step() {
		li("sheep step");
		Position currentPos = e.getCurrentPosition();
		if(currentPos == null) {
			return;
		}

		Set<EntityProxy<?>> entitiesHere = e.getEntitiesAt(currentPos);
		for(EntityProxy<?> entity : entitiesHere) {
			if(entity instanceof GrassAgent && ((GrassAgent) entity).isGrown()) {
				li("sheep eats grass [] at []", entity.getEntityName(), currentPos);
				((GrassAgent) entity).postAgentEvent(new AgentWave("EAT"));
			}
		}

		Set<Position> passableNeighbors = e.getPassableNeighborPositions(currentPos,
				entity -> entity instanceof GrassAgent);
		if(passableNeighbors.isEmpty()) {
			return;
		}

		// Look for nearest grown grass within vision range
		@SuppressWarnings("unchecked")
		Topology<Position> topology = (Topology<Position>) e.getTopology();
		Map<Position, Set<EntityProxy<?>>> visible = e.observe(visionRange);
		Position nearestTarget = null;
		int nearestDist = Integer.MAX_VALUE;
		for(Map.Entry<Position, Set<EntityProxy<?>>> entry : visible.entrySet()) {
			for(EntityProxy<?> entity : entry.getValue()) {
				if(entity instanceof GrassAgent && ((GrassAgent) entity).isGrown()) {
					int dist = topology.getDistance(currentPos, entry.getKey());
					if(dist < nearestDist) {
						nearestDist = dist;
						nearestTarget = entry.getKey();
					}
				}
			}
		}

		if(nearestTarget != null) {
			// Move towards nearest grass
			Position bestNeighbor = null;
			int bestDist = Integer.MAX_VALUE;
			for(Position neighbor : passableNeighbors) {
				int dist = topology.getDistance(neighbor, nearestTarget);
				if(dist < bestDist) {
					bestDist = dist;
					bestNeighbor = neighbor;
				}
			}
			if(bestNeighbor != null) {
				e.moveToPosition(bestNeighbor);
				return;
			}
		}

		// Random movement if outside of vision
		List<Position> passableList = new ArrayList<>(passableNeighbors);
		Position newPos = passableList.get(random.nextInt(passableList.size()));
		e.moveToPosition(newPos);
	}
	
	@Override
	public String getEntityName() {
		return getName() != null ? getName() : "Sheep";
	}
}
