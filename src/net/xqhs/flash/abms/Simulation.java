package net.xqhs.flash.abms;

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;

public class Simulation extends Node {
	// protected Topology<P> topology;

	protected Set<SimulationContext>	simulationContexts	= new HashSet<>();
	protected Set<Entity<?>>			simulationObjects	= new HashSet<>();
	
	@Override
	public void registerEntity(String entityType, Entity<?> entity, String entityName) {
		super.registerEntity(entityType, entity, entityName);
		if(entity instanceof SimulationContext)
			simulationContexts.add((SimulationContext) entity);
		else
			simulationObjects.add(entity);
	}
	
}
