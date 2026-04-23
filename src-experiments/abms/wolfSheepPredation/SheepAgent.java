package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class SheepAgent extends BaseAgent implements SteppableEntity, ShardContainer {

    protected static final String VISION_RANGE_PARAM = "visionRange";

    protected EnvironmentLinkShard e = new EnvironmentLinkShard();
    protected int visionRange = 2;
    boolean alertReceived = false;
    protected Simulation simulation;

    public void setVisionRange(int visionRange) {
        this.visionRange = visionRange;
    }

    public SheepAgent() {
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
                String content = event.get("content");
                if ("DESTROY".equals(content)) {
                    li("[] is being eaten, deregistering", getEntityName());
                    if (simulation != null)
                        simulation.deregisterEntity((Entity<?>) this);
                    return true;
                }
                alertReceived = true;
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        e.addGeneralContext(context);
        return super.addGeneralContext(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public void step() {
        li("sheep step");
        Position currentPos = e.getCurrentPosition();
        if (currentPos == null) {
            alertReceived = false;
            return;
        }
        Set<EntityProxy<?>> entitiesHere = e.getEntitiesAt(currentPos);
        for (EntityProxy<?> entity : entitiesHere) {
            if (entity instanceof GrassPatch && ((GrassPatch) entity).isGrown()) {
                li("sheep eats grass [] at []", entity.getEntityName(), currentPos);
                e.sendWaveTo(entity, new AgentWave("EAT"));
            }
        }

        if (alertReceived) {
            li("[] received danger alert from neighbor", getEntityName());
        }

        boolean wolfVisible = false;
        for (EntityProxy<?> ep : e.getEntitiesInVicinity())
            if (ep.getEntityName() != null && ep.getEntityName().toLowerCase().startsWith("wolf")) {
                wolfVisible = true;
                break;
            }

        if (wolfVisible) {
            li("[] spots a wolf nearby, broadcasting", getEntityName());
            e.broadcast(new AgentWave("wolf-alert"));
        }

        Set<Position> freeNeighbors = e.getFreeNeighborPositions(currentPos);
        if (freeNeighbors.isEmpty()) {
            alertReceived = false;
            return;
        }

        if (wolfVisible || alertReceived)
            li("[] is running away", getEntityName());

        List<Position> freeList = new ArrayList<>(freeNeighbors);

        Set<Position> passableNeighbors = e.getPassableNeighborPositions(currentPos,
                entity -> entity instanceof GrassPatch);
        if (passableNeighbors.isEmpty()) {
            alertReceived = false;
            return;
        }
        // Look for nearest grown grass within vision range
        @SuppressWarnings("unchecked")
        Topology<Position> topology = (Topology<Position>) e.getTopology();
        Map<Position, Set<EntityProxy<?>>> visible = e.observe(visionRange);
        Position nearestTarget = null;
        int nearestDist = Integer.MAX_VALUE;
        for (Map.Entry<Position, Set<EntityProxy<?>>> entry : visible.entrySet()) {
            for (EntityProxy<?> entity : entry.getValue()) {
                if (entity instanceof GrassPatch && ((GrassPatch) entity).isGrown()) {
                    int dist = topology.getDistance(currentPos, entry.getKey());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestTarget = entry.getKey();
                    }
                }
            }
        }

        if (nearestTarget != null) {
            // Move towards nearest grass
            Position bestNeighbor = null;
            int bestDist = Integer.MAX_VALUE;
            for (Position neighbor : passableNeighbors) {
                int dist = topology.getDistance(neighbor, nearestTarget);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestNeighbor = neighbor;
                }
            }
            if (bestNeighbor != null) {
                e.moveToPosition(bestNeighbor);
                alertReceived = false;
                return;
            }
        }

        // Random movement if outside of vision
        List<Position> passableList = new ArrayList<>(passableNeighbors);
        Position newPos = passableList.get(e.nextInt(passableList.size()));
        e.moveToPosition(newPos);
        alertReceived = false;
    }

    @Override
    public String getEntityName() {
        return getName() != null ? getName() : "Sheep";
    }
}
