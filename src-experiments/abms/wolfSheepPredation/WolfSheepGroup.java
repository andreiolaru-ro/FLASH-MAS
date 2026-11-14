package abms.wolfSheepPredation;

import java.util.List;

import net.xqhs.flash.abms.EntityGroup;
import net.xqhs.flash.core.Entity;

public class WolfSheepGroup extends EntityGroup {
	//this is the class (group) that displays the grid (like printMap() in RandomPositionGenerator)
	private static final long serialVersionUID = 1L;
	private int width;
	private int height;

	public WolfSheepGroup(List<Entity<?>> entityList) {
		super(entityList);
	}
	
	@Override
	protected void display() {
//		if (simulation == null || width <= 0 || height <= 0) {
//			return;
//		}
//		for (int y = 0; y < height; y++) {
//			StringBuilder stringBuilder = new StringBuilder();
//			for (int x = 0; x < width; x++) {
//				GridPosition pos = new GridPosition(x, y);
//				Object agent = simulation.getAgentAt(pos);
//				if (agent instanceof SheepAgent) stringBuilder.append("S");
//				else if (agent instanceof WolfAgent) stringBuilder.append("W");
//				else stringBuilder.append(".");
//			}
//			lf(stringBuilder.toString());
//		}
	}
}
