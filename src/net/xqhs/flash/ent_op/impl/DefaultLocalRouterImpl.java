package net.xqhs.flash.ent_op.impl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.EntityCore;
import net.xqhs.flash.ent_op.entities.Pylon;
import net.xqhs.flash.ent_op.impl.operations.RouteOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.Wave;

public class DefaultLocalRouterImpl extends EntityCore implements LocalRouter {
	
	/**
	 * The default name for entity tools instances of this implementation.
	 */
	protected static final String DEFAULT_LOCAL_ROUTER_NAME = "local router";
	
	/**
	 * The framework instance.
	 */
	protected FMas fMas;
	
	/**
	 * Added pylons used for external routing.
	 */
	protected Set<Pylon> pylons = new LinkedHashSet<>();
	
	@Override
	public boolean setup(MultiTreeMap configuration, FMas fmas) {
		setUnitName(DEFAULT_LOCAL_ROUTER_NAME);
		entityID = new EntityID(DEFAULT_LOCAL_ROUTER_NAME);
		fMas = fmas;
		return true;
	}
	
	@Override
	public void route(Wave wave) {
		var targetEntityId = wave.getTargetEntity().ID;
		
		if(fMas.entityExistsOnLocalNode(targetEntityId)) {
			routeInternalWave(wave);
		}
		else {
			routeExternalWave(wave);
		}
	}
	
	private void routeInternalWave(Wave wave) {
		var targetEntityName = wave.getTargetEntity().ID;
		var entityTools = fMas.getEntityTools(targetEntityName);
		if(entityTools != null) {
			entityTools.handleIncomingWave(wave);
			li("The wave was successfully routed.");
		}
		else
			le("The wave [] cannot be routed internally because the target entity [] cannot be found.", wave,
					targetEntityName);
	}
	
	/**
	 * Check each of the pylons if they support communication for that specific agent. TODO: check for all future types
	 * of pylons
	 * 
	 * @param wave
	 *            The wave to route.
	 */
	private void routeExternalWave(Wave wave) {
		var targetEntity = wave.getTargetEntity().ID;
		var routerEntities = fMas.routerEntities();
		routerEntities.stream().filter(ent -> ent.canRoute(wave.getTargetEntity())).findFirst()
				.ifPresentOrElse((ent) -> {
					ent.handleIncomingOperationCall(new OperationCallWave(null, null,
							RouteOperation.ROUTE_OPERATION, false, Arrays.asList(wave)));
					li("The wave was successfully routed. Found a [] pylon to route the wave to [].", ent.getID(),
							targetEntity);
				}, () -> {
					le("The wave couldn't be routed. Failed to find a pylon to route the wave to [].", targetEntity);
				});
	}
}
