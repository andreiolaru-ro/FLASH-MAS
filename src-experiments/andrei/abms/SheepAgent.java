package andrei.abms;

import andrei.abms.gridworld.GridMap;
import andrei.abms.gridworld.GridPosition;
import net.xqhs.flash.core.agent.BaseAgent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SheepAgent extends BaseAgent implements StepAgent {
    private final GridMap map;
    private GridPosition gridPosition;
    private final int mapSize;
    private final Random random = new Random();

    public SheepAgent(GridMap map, GridPosition gridPosition, int mapSize) {
        this.map = map;
        this.gridPosition = gridPosition;
        this.mapSize = mapSize;
    }

    @Override
    public boolean start() {
        super.start();
        return true;
    }

    @Override
    public void preStep() {
    }

    @Override
    public void step() {
        if (map == null || gridPosition == null) return;

        Set<GridPosition> neighbors = map.getVicinity(gridPosition);
        List<GridPosition> free = new ArrayList<>();

        for (GridPosition n : neighbors) {
            if (n.getX() < 0 || n.getX() >= mapSize ||
                    n.getY() < 0 || n.getY() >= mapSize) {
                continue;
            }

            if (map.get(n) == null) {
                free.add(n);
            }
        }

        if (!free.isEmpty()) {
            GridPosition newPos = free.get(random.nextInt(free.size()));
            map.place(this, newPos);
            map.remove(this.gridPosition);
            this.gridPosition = newPos;
        }
    }

}
