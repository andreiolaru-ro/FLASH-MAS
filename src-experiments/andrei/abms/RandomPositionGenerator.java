package andrei.abms;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
                map.place(new SheepAgent(), pos);
                placedSheep++;
            } else if (placedWolves < wolfCount) {
                map.place(new WolfAgent(), pos);
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
        DistributionConfig config = new DistributionConfig(sheep + wolves, squareSize);
        populateGridWithRandomAgents(config, map, sheep, wolves);
        System.out.println("Map looks like this:");
        for (int y = 0; y < squareSize; y++) {
            for (int x = 0; x < squareSize; x++) {
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
