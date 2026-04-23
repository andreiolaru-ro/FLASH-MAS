package net.xqhs.flash.abms;

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;

public class Simulation extends Node implements EntityProxy<Simulation> {
	// protected Topology<P> topology;
	
	protected Set<SimulationContext>	simulationContexts	= new HashSet<>();
	protected Set<Entity<?>>			simulationObjects	= new HashSet<>();
	protected SimulationExecutor		executor;
	
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
		li("Starting simulation with [] contexts and [] objects.", simulationContexts.size(), simulationObjects);
		// return executor.start();
		return true;
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
