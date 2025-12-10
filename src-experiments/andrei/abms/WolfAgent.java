package andrei.abms;

import andrei.abms.gridworld.GridMap;
import andrei.abms.gridworld.GridPosition;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.support.Pylon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WolfAgent implements StepAgent {
    private final GridMap map;
    private GridPosition gridPosition;
    private final Random random = new Random();
    private final int mapSize;

    public WolfAgent(GridMap map, GridPosition gridPosition, int mapSize) {
        this.map = map;
        this.gridPosition = gridPosition;
        this.mapSize = mapSize;
    }
    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return null;
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
                continue; //ignore outside of bounds "movement"
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
