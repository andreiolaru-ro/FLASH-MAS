package net.xqhs.flash.ent_op.entities;

import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.Relation.RelationChangeType;
import net.xqhs.util.logging.Unit;

/**
 * A very basic implementation for an entity, handling the connection to the framework, the running state, and the ID of
 * the entity.
 * 
 * @author Andrei Olaru
 */
public class EntityCore extends Unit implements EntityAPI {
	
	/**
	 * Indicates whether the implementation is currently running.
	 */
	protected boolean isRunning;
	
	/**
	 * The id of this instance.
	 */
	protected EntityID entityID;
	
	/**
	 * The corresponding entity tools for this instance.
	 */
	protected OutboundEntityTools framework;
	
	@Override
	public boolean setup(MultiTreeMap configuration) {
		if(configuration == null)
			return false;
		entityID = new EntityID(configuration.getAValue(ENTITY_ID_ATTRIBUTE_NAME));
		setUnitName(entityID.toString());
		return false;
	}
	
	@Override
	public boolean connectTools(OutboundEntityTools entityTools) {
		framework = entityTools;
		return false;
	}
	
	@Override
	public boolean start() {
		isRunning = true;
		return true;
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public Object handleIncomingOperationCall(OperationCallWave operationCall) {
		return null;
	}
	
	@Override
	public boolean handleRelationChange(RelationChangeType changeType, Relation relation) {
		return true;
	}
	
	@Override
	public boolean canRoute(EntityID destinationID) {
		return false;
	}
	
	@Override
	public EntityID getID() {
		return entityID;
	}
	
}
