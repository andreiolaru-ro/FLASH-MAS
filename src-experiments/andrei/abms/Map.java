package andrei.abms;

import java.util.Set;

import andrei.abms.gridworld.GridPosition;
import net.xqhs.flash.core.agent.Agent;

public interface Map {
	
	Set<GridPosition> getVicinity(GridPosition pos);
	
	Set<Agent> getNeighbors(Agent agent);
}
