package net.xqhs.flash.abms.space;

import java.util.Map;
import java.util.Set;

import net.xqhs.flash.core.Entity.EntityProxy;

public interface Topology<P extends Position> {

	Set<P> getVicinity(P pos);

    /**
     * Retrieves all valid positions within a specified range from a given center.
     *
     * @param pos  the center position of the vicinity
     * @param range the maximum distance from the center position
     * @return A set comprised of all positions at a distance less than or equal to the specified range
     */
    Set<P> getVicinity(P pos, int range);

    boolean isValidPosition(P pos);

	int getDistance(P a, P b);

	String visualize(Map<P, Set<EntityProxy<?>>> entityInPosition);
}
