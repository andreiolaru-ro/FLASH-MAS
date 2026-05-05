package abms.wolfSheepPredation;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WolfAgent extends BaseAgent implements SteppableEntity, ShardContainer {

    protected static final String VISION_RANGE_PARAM = "visionRange";

    protected EnvironmentLinkShard e = new EnvironmentLinkShard();
    protected int visionRange = 2;

    public void setVisionRange(int visionRange) {
        this.visionRange = visionRange;
    }

    public WolfAgent() {
        e.addGeneralContext(this);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.containsKey(VISION_RANGE_PARAM))
            visionRange = Integer.parseInt(configuration.get(VISION_RANGE_PARAM));
        return true;
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        switch (event.getType()) {
            case AGENT_WAVE:
                return true; //since atm only sheep send waves, wolf chooses to ignore message
            default:
                break;
        }
        return false;
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
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
        li("wolf step");
        Position currentPos = e.getCurrentPosition();
        if (currentPos == null) {
            return;
        }

        Set<EntityProxy<?>> entitiesHere = e.getEntitiesAt(currentPos);
        for (EntityProxy<?> entity : entitiesHere) {
            if (entity instanceof SheepAgent) {
                li("wolf eats sheep [] at []", entity.getEntityName(), currentPos);
                e.requestDestroyAgent(entity);
            }
        }

        Set<Position> neighbors = e.getValidNeighborPositions(currentPos);
        if (neighbors.isEmpty())
            return;

        // Look for nearest sheep within vision range
        @SuppressWarnings("unchecked")
        Topology<Position> topology = (Topology<Position>) e.getTopology();
        Map<Position, Set<EntityProxy<?>>> visible = e.observe(visionRange);
        Position nearestTarget = null;
        int nearestDist = Integer.MAX_VALUE;
        for (Map.Entry<Position, Set<EntityProxy<?>>> entry : visible.entrySet()) {
            for (EntityProxy<?> entity : entry.getValue()) {
                if (entity instanceof SheepAgent) {
                    int dist = topology.getDistance(currentPos, entry.getKey());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestTarget = entry.getKey();
                    }
                }
            }
        }

        if (nearestTarget != null) {
            // Move towards nearest sheep
            Position bestNeighbor = null;
            int bestDist = Integer.MAX_VALUE;
            for (Position neighbor : neighbors) {
                int dist = topology.getDistance(neighbor, nearestTarget);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestNeighbor = neighbor;
                }
            }
            if (bestNeighbor != null) {
                e.moveToPosition(bestNeighbor);
                return;
            }
        }

        // Fallback: random movement
        List<Position> neighborList = new ArrayList<>(neighbors);
        Position newPos = neighborList.get(e.nextInt(neighborList.size()));
        e.moveToPosition(newPos);
    }

    @Override
    public String getEntityName() {
        return getName() != null ? getName() : "Wolf";
    }
}
