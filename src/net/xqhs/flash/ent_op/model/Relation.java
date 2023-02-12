package net.xqhs.flash.ent_op.model;

import java.util.Objects;

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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Relation relation = (Relation) o;
		return Objects.equals(from, relation.from) && Objects.equals(to, relation.to) && Objects.equals(relationName, relation.relationName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to, relationName);
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