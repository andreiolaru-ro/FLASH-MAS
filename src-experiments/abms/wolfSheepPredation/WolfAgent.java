package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.StepAgent;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.BaseAgent;

public class WolfAgent extends BaseAgent implements StepAgent {

	protected EnvironmentLinkShard	e		= new EnvironmentLinkShard();;
	protected final Random			random	= new Random();

    public WolfAgent() {
    }

    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		e.addGeneralContext(context);
    	return super.addGeneralContext(context);
    }

    @Override
    public void step() {
		Position currentPos = e.getCurrentPosition();
        if (currentPos == null) {
            return;
        }

        Set<GridPosition> freeNeighbors = simulation.getFreeNeighbors(currentPos);
        if (freeNeighbors.isEmpty()) {
            return;
        }

        List<GridPosition> freeList = new ArrayList<>(freeNeighbors);
        GridPosition newPos = freeList.get(random.nextInt(freeList.size()));
        simulation.moveAgent(this, newPos);
    }
}
