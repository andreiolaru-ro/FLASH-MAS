package net.xqhs.flash.abms;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;

public class Simulation extends Node implements EntityProxy<Simulation> {
	// protected Topology<P> topology;

	protected Set<SimulationContext>	simulationContexts	= new HashSet<>();
	protected Set<Entity<?>>			simulationObjects	= new HashSet<>();
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
		else
			simulationObjects.add(entity);
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

	public void deregisterEntity(Entity<?> entity) {
		simulationObjects.remove(entity);
	}

	public void deregisterEntity(EntityProxy<?> proxy) {
		simulationObjects.removeIf(entity -> entity == proxy || entity.asContext() == proxy);
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		lastInstance = this;
		li("Starting simulation with [] contexts and [] objects.", simulationContexts.size(), simulationObjects);
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
		for (SimulationContext context : simulationContexts) {
			String visualization = context.visualizeAsString();
			if (visualization != null) {
				System.out.println(visualization);
			}
		}
	}
}
