package net.xqhs.flash.abms.gridworld;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.Topology;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GridTopology extends EntityCore<Node> implements Topology<GridPosition>, EntityProxy<GridTopology> {

	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";

	private final int width;
	private final int height;

	public GridTopology() {
		this(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public GridTopology(MultiTreeMap multiTreeMap) {
		this(readWidthAndHeight(multiTreeMap, WIDTH),
				readWidthAndHeight(multiTreeMap, HEIGHT));
		super.configure(multiTreeMap);
	}

	public GridTopology(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean configure(MultiTreeMap multiTreeMap) {
		return super.configure(multiTreeMap);
	}

	private static int readWidthAndHeight(MultiTreeMap multiTreeMap, String dimension) {
		if (multiTreeMap == null || !multiTreeMap.containsKey(dimension)) {
			return Integer.MAX_VALUE;
		}
		try {
			return Integer.parseInt(multiTreeMap.getAValue(dimension));
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}
	
	@Override
	public Set<GridPosition> getVicinity(GridPosition pos) {
		return Arrays.stream(GridRelativeOrientation.values())
				.map(o -> pos.getNeighborPosition(GridOrientation.NORTH, o)).collect(Collectors.toSet());
	}

	@Override
	public boolean isValidPosition(GridPosition pos) {
		return pos != null && pos.getX() >= 0 && pos.getX() < width && pos.getY() >= 0 && pos.getY() < height;
	}

	@Override
	public boolean canMoveInOneStep(GridPosition from, GridPosition to) {
		Set<GridPosition> vicinity = getVicinity(from);
		return vicinity.contains(to);
	}

	@Override
	public <A> Set<A> getNeighbors(GridPosition pos, Function<GridPosition, A> agentAtPosition) {
		Set<GridPosition> vicinity = getVicinity(pos);
		Set<A> neighbors = new HashSet<>();
		for (GridPosition neighborPos : vicinity) {
			if (isValidPosition(neighborPos)) {
				A agent = agentAtPosition.apply(neighborPos);
				if (agent != null) {
					neighbors.add(agent);
				}
			}
		}
		return neighbors;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
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
