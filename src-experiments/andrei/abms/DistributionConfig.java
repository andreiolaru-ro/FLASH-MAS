package andrei.abms;

import java.util.Objects;

//assume top left corner of the map is (0,0)
public class DistributionConfig {

    private final int populationSize;

    private final int squareMapSize;

    public DistributionConfig(int populationSize, int squareMapSize) {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("Population size must be positive, got: " + populationSize);
        }
        if (squareMapSize <= 0) {
            throw new IllegalArgumentException("Square map size must be positive, got: " + squareMapSize);
        }
        this.populationSize = populationSize;
        this.squareMapSize = squareMapSize;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public int getSquareMapSize() {
        return squareMapSize;
    }

    @Override
    public String toString() {
        return "DistributionConfig[origin=(0,0), population=" + populationSize + ", squareMapSize=" + squareMapSize + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DistributionConfig other = (DistributionConfig) obj;
        if (populationSize != other.populationSize) return false;
        if (squareMapSize != other.squareMapSize) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(populationSize, squareMapSize);
    }
}
