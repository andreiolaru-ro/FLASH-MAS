package net.xqhs.flash.abms;

import java.util.Set;

public interface Topology<P> {

	Set<P> getVicinity(P pos);

	boolean isValidPosition(P pos);
}
