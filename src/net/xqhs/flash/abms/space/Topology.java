package net.xqhs.flash.abms.space;

import java.util.Set;

public interface Topology<P extends Position> {

	Set<P> getVicinity(P pos);

	boolean isValidPosition(P pos);
}
