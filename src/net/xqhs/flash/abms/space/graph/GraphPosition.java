package net.xqhs.flash.abms.space.graph;

import net.xqhs.flash.abms.space.Position;

public class GraphPosition implements Position {
	private final String nodeId;

	public GraphPosition(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeId() {
		return nodeId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof GraphPosition))
			return false;
		return nodeId.equals(((GraphPosition) obj).nodeId);
	}

	@Override
	public int hashCode() {
		return nodeId.hashCode();
	}

	@Override
	public String toString() {
		return "[" + nodeId + "]";
	}
}
