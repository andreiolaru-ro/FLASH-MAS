package net.xqhs.flash.abms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.support.PylonProxy;

public class Simulation extends Node implements EntityProxy<Simulation> {
	// protected Topology<P> topology;

	protected Set<SimulationContext>	simulationContexts	= new HashSet<>();
	protected Set<Entity<?>>			simulationObjects	= new HashSet<>();
	/** Simulation objects deregistered since the last {@link #clearDeregistered()}, usually none or a few. */
	protected Set<Entity<?>>			deregistered		= new HashSet<>();
	protected SimulationExecutor		executor;

	// Multi-run support: each run constructs a fresh Simulation, which registers itself
	// as the lastInstance on start() and uses the latch to signal completion.
	private static volatile Simulation lastInstance;
	private final CountDownLatch		completionLatch		= new CountDownLatch(1);
	
	@Override
	public void registerEntity(String entityType, Entity<?> entity, String entityName) {
		super.registerEntity(entityType, entity, entityName);
		if(entity instanceof SimulationContext)
			simulationContexts.add((SimulationContext) entity);
		else {
			simulationObjects.add(entity);
			if(!deregistered.isEmpty())
				deregistered.remove(entity);
		}
	}
	
	public void registerExecutor(SimulationExecutor _executor) {
		this.executor = _executor;
		lf("Executor registered");
	}
	
	public Set<SimulationContext> getSimulationContexts() {
		return simulationContexts;
	}
	
	public Set<Entity<?>> getSimulationObjects() {
		return simulationObjects;
	}

	public PylonProxy getSimulationPylonProxy() {
		return nodePylonProxy;
	}

	public void deregisterEntity(Entity<?> entity) {
		if(simulationObjects.remove(entity))
			deregistered.add(entity);
	}

	public void deregisterEntity(EntityProxy<?> proxy) {
		simulationObjects.removeIf(entity -> {
			boolean match = entity == proxy || entity.asContext() == proxy;
			if(match)
				deregistered.add(entity);
			return match;
		});
	}

	/**
	 * Tells whether an entity was deregistered since the last {@link #clearDeregistered()}.
	 */
	public boolean isDeregistered(Entity<?> entity) {
		return !deregistered.isEmpty() && deregistered.contains(entity);
	}

	/**
	 * Forgets the entities deregistered so far, see {@link #isDeregistered(Entity)}.
	 */
	public void clearDeregistered() {
		deregistered.clear();
	}
	
	/**
	 * Override in simulation to ONLY start non-steppable entities as the executor will handle the steppable ones.
	 */
	@Override
	protected void startAndRegister(List<Entity<?>> entities, boolean isNodeStart) {
		super.startAndRegister(entities.stream().filter(entity -> !(entity instanceof SteppableEntity))
				.collect(Collectors.toList()), isNodeStart);
	}

	@Override
	public boolean start() {
		if(!super.start())
			return false;
		lastInstance = this;
		// return executor.start();
		return true;
	}

	public static Simulation getLastInstance() {
		return lastInstance;
	}

	public void awaitCompletion() {
		try {
			completionLatch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	@Override
	public String getEntityName() {
		return "Simulation";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Simulation> asContext() {
		return this;
	}

	public void executionCompleted() {
		// TODO change this when making simulation the node
		completionLatch.countDown();
		stop();
	}
	
	public void stepCompleted() {
        // Deleted visualisation usage
	}
}
