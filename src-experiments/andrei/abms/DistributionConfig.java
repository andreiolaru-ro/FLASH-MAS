package andrei.abms;

import andrei.abms.gridworld.GridTopology;

import java.util.Objects;

//assume top left corner of the map is (0,0)
public class DistributionConfig {

    private final int populationSize;
    private final GridTopology topology;

    public DistributionConfig(int populationSize, GridTopology topology) {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("Population size must be positive, got: " + populationSize);
        }
        if (topology == null) {
            throw new IllegalArgumentException("Topology cannot be null");
        }
        this.populationSize = populationSize;
        this.topology = topology;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public GridTopology getTopology() {
        return topology;
    }

    public int getWidth() {
        return topology.getWidth();
    }

    public int getHeight() {
        return topology.getHeight();
    }

    public int getTotalCells() {
        return topology.getWidth() * topology.getHeight();
    }

    @Override
    public String toString() {
        return "DistributionConfig[population=" + populationSize + ", width=" + getWidth() + ", height=" + getHeight() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DistributionConfig other = (DistributionConfig) obj;
        if (populationSize != other.populationSize) return false;
        if (!topology.equals(other.topology)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(populationSize, topology);
    }
}
