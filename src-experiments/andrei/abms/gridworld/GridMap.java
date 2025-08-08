package andrei.abms.gridworld;

import java.util.Set;

import andrei.abms.Map;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GridMap extends EntityCore<Node> implements Map {
	@Override
	public boolean configure(MultiTreeMap configuration) {
		return super.configure(configuration);
	}
	
	@Override
	public Set<GridPosition> getVicinity(GridPosition pos) {
		// TODO Auto-generated method stub
		return null;
	}
}
