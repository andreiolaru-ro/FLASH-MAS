package net.xqhs.flash.abms.space.gridworld;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GridTopology implements Topology<GridPosition> {

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
	}

	public GridTopology(int width, int height) {
		this.width = width;
		this.height = height;
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

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public String visualize(Map<GridPosition, Set<EntityProxy<?>>> entityInPosition) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");

		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				GridPosition pos = new GridPosition(x, y);
				Set<EntityProxy<?>> entities = entityInPosition.get(pos);

				if (entities == null || entities.isEmpty()) {
					sb.append(". ");
				} else {
					EntityProxy<?> entity = entities.iterator().next();
					String entityName = entity.getEntityName();
					if (entityName != null && !entityName.isEmpty()) {
						char symbol = Character.toUpperCase(entityName.charAt(0));
						sb.append(symbol).append(" ");
					} else {
						sb.append("? ");
					}
				}
			}
			sb.append("\n");
		}

		return sb.toString();
	}
}
