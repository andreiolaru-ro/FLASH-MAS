package net.xqhs.flash.abms;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

public class RandomContext extends SimulationContext.BaseContext
        implements SimulationContext, EntityProxy<RandomContext> {

    protected Random random;
    /** The seed of this context */
    protected long seed;

    public RandomContext(MultiTreeMap configuration) {
        seed = new Random().nextLong();
        random = new Random(seed);
        configure(configuration);
    }

    public RandomContext(long seed) {
        this.seed = seed;
        random = new Random(seed); //for reproducible experiments
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        super.configure(configuration);
        if (configuration != null && configuration.containsKey("seed")) {
            seed = Long.parseLong(configuration.getAValue("seed"));
            random = new Random(seed);
            li("RandomContext configured with seed []", seed);
        }
        return true;
    }

    public void setSeed(long seed) {
        this.seed = seed;
        random.setSeed(seed);
    }

    public Random forEntity(String entityName) {
        return new Random(seed * 31 + (entityName == null ? 0 : entityName.hashCode()));
    }

    public int nextInt() {
        return random.nextInt();
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public float nextFloat() {
        return random.nextFloat();
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    public double nextGaussian() {
        return random.nextGaussian();
    }

    public <T> void shuffle(List<T> list) {
        Collections.shuffle(list, random);
    }

    @Override
    public void validateAndExecutePendingActions() {
        pendingActions.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Simulation>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public String getEntityName() {
        return name;
    }
}
