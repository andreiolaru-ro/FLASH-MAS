package net.xqhs.flash.abms.space;

import java.util.Map;
import java.util.Set;

import net.xqhs.flash.core.Entity.EntityProxy;

public interface Topology<P extends Position> {

	Set<P> getVicinity(P pos);

	boolean isValidPosition(P pos);

	String visualize(Map<P, Set<EntityProxy<?>>> entityInPosition);
}
