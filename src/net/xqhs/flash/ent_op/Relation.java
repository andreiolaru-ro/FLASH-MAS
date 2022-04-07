package net.xqhs.flash.ent_op;

@SuppressWarnings("javadoc")
public class Relation {
	public static enum RelationChangeType {
		CREATE, DESTROY
	}
	
	EntityID	from;
	EntityID	to;
	String		relationName;
	
	public EntityID getFrom() {
		return from;
	}
	
	public EntityID getTo() {
		return to;
	}
	
	public String getRelation() {
		return relationName;
	}
}