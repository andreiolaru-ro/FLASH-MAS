package net.xqhs.flash.ent_op.model;

import net.xqhs.flash.ent_op.model.Operation.Restriction;

import java.util.Set;

/**
 * An {@link OutboundEntityTools} instance is associated with an entity (implementing {@link EntityAPI}) in order to connect it
 * with the FLASH-MAS framework.
 * <p>
 * This interface should primarily face an Entity implementation, offering to it a variety of services, among which
 * management of the array of operations which the entity can handle, as well as routing operations towards other
 * entities.
 * 
 * @author Andrei Olaru
 */
public interface OutboundEntityTools {
	/**
	 * This should be called by an entity at {@link EntityAPI#setup} time to make the link between the entity and the
	 * {@link OutboundEntityTools} instance and to assign a name to the entity.
	 * 
	 * @param entity
	 *            - the name that the entity intends to use. <code>null</code> if the entity does not have any
	 *            preference for the name (a name will be automatically assigned).
	 * @return <code>true</code> if the link with the {@link OutboundEntityTools} is successful. <code>false</code> is returned
	 *         if the chosen name is not available.
	 */
	boolean initialize(EntityAPI entity);
	
	/**
	 * Adds a new operation to the list of operations.
	 * 
	 * @return <code>true</code> if the addition succeeded (no other operation with the same name already existed).
	 */
	boolean createOperation(Operation operation);
	
	/**
	 * Removes an operation from the list of available operations.
	 * 
	 * @param operationName
	 * @return
	 */
	boolean removeOperation(String operationName);
	
	/**
	 * @return all the relations which involve the associated entity.
	 */
	Set<Relation> getRelations();
	
	/**
	 * @return all the relations that describe how other entities are in the scope of this entity.
	 */
	Set<Relation> getIncomingRelations(); // children
	
	/**
	 * @return all the relations that describe how this entity is in the scope of other entities.
	 */
	Set<Relation> getOutgoingRelations(); // parents
	
	/**
	 * Method called by an entity to its associated {@link OutboundEntityTools} instance to issue an operation call (without
	 * expecting a result).
	 * 
	 * @param operationCall
	 */
	void handleOutgoingOperationCall(OperationCall operationCall);
	
	/**
	 * Callback for when a result of the operation is received.
	 */
	public static interface ResultReceiver {
		/**
		 * @param result
		 *            - the result of the operation.
		 */
		public void resultNotification(Object result);
	}
	
	/**
	 * Method called by an entity to its associated {@link OutboundEntityTools} instance to issue an operation call and
	 * expecting a result.
	 * 
	 * @param operationCall
	 * @param callback
	 */
	void handleOutgoingOperationCall(OperationCall operationCall, ResultReceiver callback);
	
	/**
	 * @param operationCall
	 * @param targets
	 *            - a description of the restrictions that the target entities must abide to.
	 * @param expectResults
	 * @param callback
	 */
	void broadcastOutgoingOperationCall(OperationCall operationCall, Set<Restriction> targets, boolean expectResults, ResultReceiver callback);
	
	/**
	 * Method called by an entity to its associated {@link OutboundEntityTools} instance to issue an operation call and expect
	 * until a result is received.
	 * 
	 * @param operationCall
	 * @return the result of the operation.
	 */
	Object handleOperationCallBlocking(OperationCall operationCall);
	
}
