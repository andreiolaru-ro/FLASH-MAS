package andrei.abms;

import java.util.Set;

import andrei.abms.gridworld.GridPosition;

public interface Map {
	
	Set<GridPosition> getVicinity(GridPosition pos);
}
