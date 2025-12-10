package andrei.abms;

import java.util.*;

import andrei.abms.gridworld.GridMap;
import andrei.abms.gridworld.GridPosition;

public class RandomPositionGenerator {
    private static final int MAX_ATTEMPTS = 10000;

    public static void populateGridWithRandomAgents(DistributionConfig config, GridMap map, int sheepCount, int wolfCount) {
        if (config == null) throw new IllegalArgumentException("DistributionConfig cannot be null");
        if (map == null) throw new IllegalArgumentException("Map cannot be null");
        int totalToPlace = sheepCount + wolfCount;
        if (totalToPlace > config.getSquareMapSize() * config.getSquareMapSize())
            throw new IllegalArgumentException("Cannot place more agents than cells in area");

        Set<String> used = new HashSet<>();
        Random random = new Random();
        int placedSheep = 0, placedWolves = 0, attempts = 0;
        int squareSize = config.getSquareMapSize();
        while ((placedSheep + placedWolves) < totalToPlace && attempts < MAX_ATTEMPTS) {
            attempts++;
            int x = random.nextInt(squareSize);
            int y = random.nextInt(squareSize);
            String key = x + "," + y;
            if (used.contains(key)) continue;
            used.add(key);
            GridPosition pos = new GridPosition(x, y);
            if (placedSheep < sheepCount) {
                map.place(new SheepAgent(map, pos, squareSize), pos);
                placedSheep++;
            } else if (placedWolves < wolfCount) {
                map.place(new WolfAgent(map, pos, squareSize), pos);
                placedWolves++;
            }
        }
        if ((placedSheep + placedWolves) < totalToPlace) {
            System.out.println("Unable to place all agents in given area (requested: " + totalToPlace + ", got: " + (placedSheep + placedWolves) + ")");
        }
    }

    public static void main(String[] args) {
        GridMap map = new GridMap();
        int squareSize = 8;
        int sheep = 10;
        int wolves = 5;
        int steps = 10;
        DistributionConfig config = new DistributionConfig(sheep + wolves, squareSize);

        populateGridWithRandomAgents(config, map, sheep, wolves);

        System.out.println("Initial State:");
        printMap(map, squareSize);

        for (int step = 1; step <= steps; step++) {
            System.out.println("\nStep " + step + ":");

            //get all agents to shuffle them
            List<StepAgent> agentsToMove = new ArrayList<>();

            for (int y = 0; y < squareSize; y++) {
                for (int x = 0; x < squareSize; x++) {
                    GridPosition pos = new GridPosition(x, y);
                    Object agent = map.get(pos);
                    if (agent instanceof StepAgent stepAgent) {
                        agentsToMove.add(stepAgent);
                    }
                }
            }

            Collections.shuffle(agentsToMove);//so that top left agent in (0,0) doesn't always have priority
                                                //in choosing to move to a free space

            for (StepAgent agent : agentsToMove) {
                agent.step(); //every agents moves 1 time
            }

            printMap(map, squareSize);
        }
    }
    
    private static void printMap(GridMap map, int size) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                GridPosition pos = new GridPosition(x, y);
                Object agent = map.get(pos);
                if (agent instanceof SheepAgent) System.out.print("S");
                else if (agent instanceof WolfAgent) System.out.print("W");
                else System.out.print(".");
            }
            System.out.println();
        }
    }
}
