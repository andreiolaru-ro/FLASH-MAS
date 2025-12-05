package andrei.abms;

import java.util.Objects;

public class DistributionConfig {

    private final int centerX;

    private final int centerY;

    private final int populationSize;

    private final int squareMapSize;

    public DistributionConfig(int centerX, int centerY, int populationSize, int squareMapSize) {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("Population size must be positive, got: " + populationSize);
        }
        if (squareMapSize <= 0) {
            throw new IllegalArgumentException("Square map size must be positive, got: " + squareMapSize);
        }
        this.centerX = centerX;
        this.centerY = centerY;
        this.populationSize = populationSize;
        this.squareMapSize = squareMapSize;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public int getSquareMapSize() {
        return squareMapSize;
    }

    @Override
    public String toString() {
        return "DistributionConfig[center=(" + centerX + "," + centerY + "), population=" + populationSize + ", squareMapSize=" + squareMapSize + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DistributionConfig other = (DistributionConfig) obj;
        if (centerX != other.centerX) return false;
        if (centerY != other.centerY) return false;
        if (populationSize != other.populationSize) return false;
        if (squareMapSize != other.squareMapSize) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(centerX, centerY, populationSize, squareMapSize);
    }
}
