package net.xqhs.flash.ent_op.model;

@SuppressWarnings("javadoc")
public class Relation {
	public enum RelationChangeType {
		CREATE, DESTROY
	}

	public enum RelationType {
		NODE, EXECUTES_ON, IN_SCOPE_OF
	}

	EntityID	 from;
	EntityID	 to;
	String		 relationName;
	RelationType relationType;

	public EntityID getFrom() {
		return from;
	}

	public EntityID getTo() {
		return to;
	}

	public String getRelation() {
		return relationName;
	}

	public RelationType getRelationType () {
		return relationType;
	}
}