package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.StepAgent;
import net.xqhs.flash.abms.gridworld.GridPosition;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.BaseAgent;

public class WolfAgent extends BaseAgent implements StepAgent {

    private Simulation<GridPosition> simulation;
    private final Random random = new Random();

    public WolfAgent() {
    }

    public WolfAgent(Simulation<GridPosition> simulation) {
        this.simulation = simulation;
    }

    @Override
    public boolean start() {
        super.start();
        if (simulation == null) {
            for (Entity.EntityProxy<? extends Entity<?>> c : getFullContext()) {
                if (c instanceof Simulation) {
                    simulation = (Simulation<GridPosition>) c;
                }
            }
        }
        return true;
    }

    @Override
    public void preStep() {
    }

    @Override
    public void step() {
        if (simulation == null) {
            return;
        }
        GridPosition currentPos = simulation.getAgentPosition(this);
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
