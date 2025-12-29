package andrei.abms;

import java.util.Set;
import java.util.function.Function;

public interface Topology<P> {

	Set<P> getVicinity(P pos);

	boolean isValidPosition(P pos);

	boolean canMoveInOneStep(P from, P to);

	<A> Set<A> getNeighbors(P pos, Function<P, A> agentAtPosition);
}
