package net.xqhs.flash.ent_op;

/**
 * An {@link EntityTools} instance is associated with an entity (implementing {@link Entity}) in order to connect it
 * with the FLASH-MAS framework.
 * <p>
 * This interface should primarily face an Entity implementation, offering to it a variety of services, among which
 * management of the array of operations which the entity can handle, as well as routing operations towards other
 * entities.
 * 
 * @author Andrei Olaru
 */
public interface EntityTools {
	/**
	 * This should be called by an entity at {@link Entity#setup} time to make the link between the entity and the
	 * {@link EntityTools} instance and to assign a name to the entity.
	 * 
	 * @param name
	 *            - the name that the entity intends to use. <code>null</code> if the entity does not have any
	 *            preference for the name (a name will be automatically assigned).
	 * @return <code>true</code> if the link with the {@link EntityTools} is successful. <code>false</code> is returned
	 *         if the chosen name is not available.
	 */
	boolean initialize(String name);
}
