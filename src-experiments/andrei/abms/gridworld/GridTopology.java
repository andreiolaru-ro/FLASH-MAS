package andrei.abms.gridworld;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import andrei.abms.Topology;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GridTopology extends EntityCore<Node> implements Topology, EntityProxy<GridTopology> {
	
	java.util.Map<GridPosition, Agent> positions = new HashMap<>();

	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		return true;
	}
	
	@Override
	public Set<GridPosition> getVicinity(GridPosition pos) {
		return Arrays.stream(GridRelativeOrientation.values())
				.map(o -> pos.getNeighborPosition(GridOrientation.NORTH, o)).collect(Collectors.toSet());
	}
	
	@Override
	public Set<Agent> getNeighbors(Agent agent) {
		GridPosition pos = positions.entrySet().stream().filter(e -> e.getValue().equals(agent))
				.map(java.util.Map.Entry::getKey).findFirst().orElse(null);
		if(pos == null)
			return null;
		// getVicinity(pos).stream().map(p -> positions.get(pos)).collect(Collectors.toSet());
		Set<GridPosition> vicinity = getVicinity(pos);
		Set<Agent> ret = new HashSet<>();
		for(GridPosition p : vicinity)
			if(positions.get(p) != null)
				ret.add(positions.get(p));
		return ret;
	}
	
	public boolean place(Agent agent, GridPosition position) {
		positions.put(position, agent);
		return true;
	}

    public boolean remove(GridPosition position) {
        positions.remove(position);
        return true;
    }
	
	public Agent get(GridPosition pos) {
		return positions.get(pos);
	}
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
	@Override
	public EntityProxy<GridTopology> asContext() {
		return this;
	}
}
