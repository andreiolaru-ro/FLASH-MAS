package abms.wolfSheepPredation;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.StepAgent;
import net.xqhs.flash.abms.gridworld.GridPosition;
import net.xqhs.flash.core.agent.BaseAgent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WolfAgent extends BaseAgent implements StepAgent {
    private final Simulation<GridPosition> simulation;
    private final Random random = new Random();

    public WolfAgent(Simulation<GridPosition> simulation) {
        this.simulation = simulation;
    }

    @Override
    public void preStep() {
    }

    @Override
    public void step() {
        GridPosition currentPos = simulation.getAgentPosition(this);
        if (currentPos == null) return;

        Set<GridPosition> freeNeighbors = simulation.getFreeNeighbors(currentPos);
        if (freeNeighbors.isEmpty()) return;

        List<GridPosition> freeList = new ArrayList<>(freeNeighbors);
        GridPosition newPos = freeList.get(random.nextInt(freeList.size()));
        simulation.moveAgent(this, newPos);
    }
}
