package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.support.Pylon;

public class SheepAgent extends BaseAgent implements SteppableEntity, EntityProxy<BaseAgent> {

    protected EnvironmentLinkShard e = new EnvironmentLinkShard();
    protected final Random random = new Random();

    public SheepAgent() {
        e.addGeneralContext(this);
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
        Position currentPos = e.getCurrentPosition();
        if (currentPos == null)
            return;

        List<AgentWave> alerts = e.clearWaves();
        boolean alertReceived = !alerts.isEmpty();
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
        if (freeNeighbors.isEmpty())
            return;

        if (wolfVisible || alertReceived)
            li("[] is running away", getEntityName());

        List<Position> freeList = new ArrayList<>(freeNeighbors);
        Position newPos = freeList.get(random.nextInt(freeList.size()));
        e.moveToPosition(newPos);
    }

    @Override
    public String getEntityName() {
        return getName() != null ? getName() : "Sheep";
    }
}
