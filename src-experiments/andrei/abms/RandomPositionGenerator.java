package andrei.abms;

import java.util.*;

import andrei.abms.gridworld.GridTopology;
import andrei.abms.gridworld.GridPosition;

public class RandomPositionGenerator {
    private static final int MAX_ATTEMPTS = 10000;

    public static void populateGridWithRandomAgents(DistributionConfig config, Simulation<GridPosition> simulation, int sheepCount, int wolfCount) {
        if (config == null) throw new IllegalArgumentException("DistributionConfig cannot be null");
        if (simulation == null) throw new IllegalArgumentException("Simulation cannot be null");
        int totalToPlace = sheepCount + wolfCount;
        if (totalToPlace > config.getTotalCells())
            throw new IllegalArgumentException("Cannot place more agents than cells in area");

        Set<String> used = new HashSet<>();
        Random random = new Random();
        int placedSheep = 0, placedWolves = 0, attempts = 0;
        int width = config.getWidth();
        int height = config.getHeight();
        while ((placedSheep + placedWolves) < totalToPlace && attempts < MAX_ATTEMPTS) {
            attempts++;
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            String key = x + "," + y;
            if (used.contains(key)) continue;
            used.add(key);
            GridPosition pos = new GridPosition(x, y);
            if (placedSheep < sheepCount) {
                SheepAgent agent = new SheepAgent(simulation);
                simulation.placeAgent(agent, pos);
                placedSheep++;
            } else if (placedWolves < wolfCount) {
                WolfAgent agent = new WolfAgent(simulation);
                simulation.placeAgent(agent, pos);
                placedWolves++;
            }
        }
        if ((placedSheep + placedWolves) < totalToPlace) {
            System.out.println("Unable to place all agents in given area (requested: " + totalToPlace + ", got: " + (placedSheep + placedWolves) + ")");
        }
    }

    public static void main(String[] args) {
        int width = 8;
        int height = 4;
        int sheep = 10;
        int wolves = 5;
        int steps = 10;

        GridTopology topology = new GridTopology(width, height);
        Simulation<GridPosition> simulation = new Simulation<>(topology);
        DistributionConfig config = new DistributionConfig(sheep + wolves, topology);

        populateGridWithRandomAgents(config, simulation, sheep, wolves);

        System.out.println("Initial State:");
        printMap(simulation, width, height);

        for (int step = 1; step <= steps; step++) {
            System.out.println("\nStep " + step + ":");

            List<StepAgent> agentsToMove = new ArrayList<>(simulation.getAllAgents());

            Collections.shuffle(agentsToMove);

            agentsToMove.forEach(StepAgent::step);

            printMap(simulation, width, height);
        }
    }
    
    private static void printMap(Simulation<GridPosition> simulation, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                GridPosition pos = new GridPosition(x, y);
                Object agent = simulation.getAgentAt(pos);
                if (agent instanceof SheepAgent) System.out.print("S");
                else if (agent instanceof WolfAgent) System.out.print("W");
                else System.out.print(".");
            }
            System.out.println();
        }
    }
}
