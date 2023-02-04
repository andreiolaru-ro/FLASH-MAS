package net.xqhs.flash.ent_op.model;

@SuppressWarnings("javadoc")
public class Relation {
	public enum RelationChangeType {
		CREATE, REMOVE
	}

	public enum RelationChangeResult {
		APPROVED, REJECTED
	}

	public enum RelationType {
		NODE, EXECUTES_ON, IN_SCOPE_OF
	}

	EntityID	 from;
	EntityID	 to;
	String		 relationName;

	public Relation() {

	}

	public Relation(EntityID from, EntityID to, String relationName) {
		this.from = from;
		this.to = to;
		this.relationName = relationName;
	}

	public EntityID getFrom() {
		return from;
	}

	public EntityID getTo() {
		return to;
	}

	public String getRelationName() {
		return relationName;
	}

	@Override
	public String toString() {
		return "Relation{" +
				"from=" + from +
				", to=" + to +
				", relationName='" + relationName + '\'' +
				'}';
	}
}