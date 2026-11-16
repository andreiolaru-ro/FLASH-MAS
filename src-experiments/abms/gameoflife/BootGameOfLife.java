package abms.gameoflife;

import net.xqhs.flash.FlashBoot;

/**
 * Boot class to initialize and run an automatic 12x12 Grid for Conway's Game of Life
 * featuring a moving Glider pattern using the StepWise ABMS framework.
 */
public class BootGameOfLife {

	public static void main(String[] args) {
		StringBuilder a = new StringBuilder();

		int steps = 25;
		a.append(" -load_order executor;context;agent");
		a.append(" -package abms.gameoflife abms.samples testing net.xqhs.flash.abms");
		a.append(" -node sim classpath:Simulation");
		a.append(" -executor StepWise:StepWise steps:").append(steps);
		a.append(" -context AgentManagement:agentManagement");
		a.append(" -context GeneralCommunication:communication");

		int rows = 12;
		int cols = 12;

		a.append(" -agent gridMonitor classpath:abms.gameoflife.GridMonitor rows:").append(rows).append(" cols:").append(cols);

		boolean[][] initialGrid = new boolean[rows][cols];

		//  [ ] [ ] [ ]
		//  [0] [0] [0]
		//  [ ] [ ] [ ]
		int startX = 1;
		int startY = 1;
		initialGrid[startX + 1][startY] = true;
		initialGrid[startX + 1][startY + 2] = true;
		initialGrid[startX + 1][startY + 1] = true;

		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				String cellName = "cell_" + r + "_" + c;
				boolean isAlive = initialGrid[r][c];

				a.append(" -agent ").append(cellName)
						.append(" classpath:abms.gameoflife.CellAgent")
						.append(" initialState:").append(isAlive);

				a.append(" monitor:gridMonitor");

				for (int dr = -1; dr <= 1; dr++) {
					for (int dc = -1; dc <= 1; dc++) {
						if (dr == 0 && dc == 0) continue;

						int nr = r + dr;
						int nc = c + dc;

						if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
							String neighborName = "cell_" + nr + "_" + nc;
							a.append(" neighbor:").append(neighborName);
						}
					}
				}
			}
		}

		FlashBoot.main(a.toString().trim().split("\\s+"));
	}
}