package net.xqhs.flash.ent_op.model;

/**
 * Stores the identifier of an entity.
 * 
 * @author Andrei Olaru
 */
public class EntityID {
	/**
	 * The name of the attribute which contains the entity id.
	 */
	public static final String	ENTITY_ID_ATTRIBUTE_NAME = "entity-id";
	/**
	 * A placeholder ID to be used, for example, when specifying the <i>caller entity</i> in the restriction on an
	 * operations. The {@link #SUBJECT_ID} does not have a hash code and is not equal to itself.
	 */
	public static EntityID SUBJECT_ID = new EntityID(null);
	
	/**
	 * The identifier, as a URI.
	 */
	public String ID;
	
	/**
	 * Default constructor.
	 * 
	 * @param URI
	 */
	public EntityID(String URI) {
		ID = URI;
	}
	
	@Override
	public int hashCode() {
		if(ID == null)
			throw new UnsupportedOperationException("Cannot compute a hash code for the null ID.");
		return ID.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj instanceof EntityID)
			return ID.equals(((EntityID) obj).ID);
		return false;
	}
	
	@Override
	public String toString() {
		return "<" + toString() + ">";
	}
}
